package com.villamil.barberbooking.application.service;

import java.time.LocalDate;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.CreatePublicAppointmentCommand;
import com.villamil.barberbooking.application.dto.command.GetBarberAvailabilityCommand;
import com.villamil.barberbooking.application.dto.response.BarberAvailabilityResponse;
import com.villamil.barberbooking.application.dto.response.PublicAppointmentResponse;
import com.villamil.barberbooking.application.dto.response.PublicBarberResponse;
import com.villamil.barberbooking.application.dto.response.PublicServiceOfferingResponse;
import com.villamil.barberbooking.application.exception.IdempotencyConflictException;
import com.villamil.barberbooking.application.exception.InvalidIdempotencyKeyException;
import com.villamil.barberbooking.application.exception.MissingIdempotencyKeyException;
import com.villamil.barberbooking.application.idempotency.PublicBookingIdempotencyRecord;
import com.villamil.barberbooking.application.port.in.CreatePublicAppointmentUseCase;
import com.villamil.barberbooking.application.port.in.GetPublicBarberAvailabilityUseCase;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.application.port.out.PublicBarberShopRepositoryPort;
import com.villamil.barberbooking.application.port.out.PublicBookingIdempotencyPort;
import com.villamil.barberbooking.application.port.out.TenantContextExecutor;
import com.villamil.barberbooking.application.tenant.PublicTenantContext;
import com.villamil.barberbooking.domain.exception.PublicResourceNotFoundException;
import com.villamil.barberbooking.domain.exception.ResourceInactiveException;
import com.villamil.barberbooking.domain.model.Appointment;
import com.villamil.barberbooking.domain.model.Customer;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

@Service
class PublicBookingService implements GetPublicBarberAvailabilityUseCase, CreatePublicAppointmentUseCase {
	private static final Pattern IDEMPOTENCY_KEY_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{8,128}");

	private final PublicBarberShopRepositoryPort publicBarberShopRepositoryPort;
	private final TenantContextExecutor tenantContextExecutor;
	private final BarberAvailabilityCalculator barberAvailabilityCalculator;
	private final CustomerRepositoryPort customerRepositoryPort;
	private final AppointmentBookingPolicy appointmentBookingPolicy;
	private final AppointmentRepositoryPort appointmentRepositoryPort;
	private final PublicBookingIdempotencyPort publicBookingIdempotencyPort;
	private final PublicBookingRequestHasher publicBookingRequestHasher;
	private final PublicBookingRateLimiter publicBookingRateLimiter;

	PublicBookingService(
			PublicBarberShopRepositoryPort publicBarberShopRepositoryPort,
			TenantContextExecutor tenantContextExecutor,
			BarberAvailabilityCalculator barberAvailabilityCalculator,
			CustomerRepositoryPort customerRepositoryPort,
			AppointmentBookingPolicy appointmentBookingPolicy,
			AppointmentRepositoryPort appointmentRepositoryPort,
			PublicBookingIdempotencyPort publicBookingIdempotencyPort,
			PublicBookingRequestHasher publicBookingRequestHasher,
			PublicBookingRateLimiter publicBookingRateLimiter
	) {
		this.publicBarberShopRepositoryPort = publicBarberShopRepositoryPort;
		this.tenantContextExecutor = tenantContextExecutor;
		this.barberAvailabilityCalculator = barberAvailabilityCalculator;
		this.customerRepositoryPort = customerRepositoryPort;
		this.appointmentBookingPolicy = appointmentBookingPolicy;
		this.appointmentRepositoryPort = appointmentRepositoryPort;
		this.publicBookingIdempotencyPort = publicBookingIdempotencyPort;
		this.publicBookingRequestHasher = publicBookingRequestHasher;
		this.publicBookingRateLimiter = publicBookingRateLimiter;
	}

	@Override
	@Transactional(readOnly = true)
	public BarberAvailabilityResponse getAvailability(
			String companySlug,
			String branchSlug,
			Long barberId,
			Long serviceOfferingId,
			LocalDate date
	) {
		PublicTenantContext tenant = resolveTenant(companySlug, branchSlug);
		requireVisibleService(tenant, serviceOfferingId);
		requireVisibleBarber(tenant, barberId);
		return tenantContextExecutor.withTenant(
				tenant.toTenantContext(),
				() -> barberAvailabilityCalculator.calculate(
						new GetBarberAvailabilityCommand(barberId, serviceOfferingId, date)
				)
		);
	}

	@Override
	@Transactional
	public PublicAppointmentResponse create(CreatePublicAppointmentCommand command) {
		String idempotencyKey = validateIdempotencyKey(command.idempotencyKey());
		PublicTenantContext tenant = resolveTenant(command.companySlug(), command.branchSlug());
		Customer requestedCustomer = Customer.create(
				command.customer().fullName(),
				command.customer().phone(),
				command.customer().email()
		);

		String requestHash = publicBookingRequestHasher.hash(command, tenant.companyId(), tenant.branchId());

		return tenantContextExecutor.withTenant(tenant.toTenantContext(), () -> {
			String claimToken = publicBookingIdempotencyPort.tryStart(idempotencyKey, requestHash);
			if (claimToken == null) {
				return replayExisting(idempotencyKey, requestHash, tenant, command, requestedCustomer);
			}

			publicBookingRateLimiter.check(
					command.remoteAddress(), tenant.companyId(), tenant.branchId(), requestedCustomer.phone()
			);
			PublicServiceOfferingResponse service = requireVisibleService(tenant, command.serviceOfferingId());
			PublicBarberResponse barber = requireVisibleBarber(tenant, command.barberId());
			Customer customer = findOrCreateCustomer(requestedCustomer);
			Appointment appointment = appointmentBookingPolicy.createValidatedAppointment(
					customer.id(),
					command.barberId(),
					command.serviceOfferingId(),
					command.startAt(),
					AppointmentSource.ONLINE,
					AppointmentStatus.SCHEDULED
			);
			Appointment savedAppointment = appointmentRepositoryPort.save(appointment);
			publicBookingIdempotencyPort.complete(idempotencyKey, claimToken, savedAppointment.id());
			return PublicAppointmentResponse.from(
					savedAppointment,
					service,
					barber,
					requestedCustomer.fullName(),
					requestedCustomer.phone()
			);
		});
	}

	private PublicAppointmentResponse replayExisting(
			String idempotencyKey,
			String requestHash,
			PublicTenantContext tenant,
			CreatePublicAppointmentCommand command,
			Customer requestedCustomer
	) {
		PublicBookingIdempotencyRecord existing = publicBookingIdempotencyPort.find(idempotencyKey)
				.orElseThrow(() -> new IdempotencyConflictException("Idempotency key is currently unavailable"));
		if (!existing.requestHash().equals(requestHash)) {
			throw new IdempotencyConflictException("Idempotency key was already used with a different request");
		}
		if (existing.status() != PublicBookingIdempotencyRecord.Status.COMPLETED
				|| existing.appointmentId() == null) {
			throw new IdempotencyConflictException("Idempotent request is still in progress");
		}
		Appointment appointment = appointmentRepositoryPort.findById(existing.appointmentId())
				.orElseThrow(() -> new IllegalStateException("Idempotent appointment was not found"));
		PublicServiceOfferingResponse service = publicBarberShopRepositoryPort.findServiceSnapshotByCompanyId(
				tenant.companyId(), command.serviceOfferingId()
		)
				.orElseThrow(() -> new IllegalStateException("Idempotent service snapshot was not found"));
		PublicBarberResponse barber = publicBarberShopRepositoryPort.findBarberSnapshotByTenant(
				tenant.companyId(), tenant.branchId(), command.barberId()
		)
				.orElseThrow(() -> new IllegalStateException("Idempotent barber snapshot was not found"));
		return PublicAppointmentResponse.from(
				appointment,
				service,
				barber,
				requestedCustomer.fullName(),
				requestedCustomer.phone()
		);
	}

	private String validateIdempotencyKey(String value) {
		if (value == null || value.isBlank()) {
			throw new MissingIdempotencyKeyException("Idempotency-Key header is required");
		}
		String normalized = value.strip();
		if (!IDEMPOTENCY_KEY_PATTERN.matcher(normalized).matches()) {
			throw new InvalidIdempotencyKeyException(
					"Idempotency-Key must be 8 to 128 characters using letters, numbers, '.', '_', ':', or '-'"
			);
		}
		return normalized;
	}

	private PublicTenantContext resolveTenant(String companySlug, String branchSlug) {
		return publicBarberShopRepositoryPort.findActiveTenantBySlugs(
				normalizeSlug(companySlug),
				normalizeSlug(branchSlug)
		)
				.orElseThrow(() -> new PublicResourceNotFoundException("Public branch not found"));
	}

	private PublicServiceOfferingResponse requireVisibleService(PublicTenantContext tenant, Long serviceOfferingId) {
		return publicBarberShopRepositoryPort.findVisibleServiceByCompanyId(tenant.companyId(), serviceOfferingId)
				.orElseThrow(() -> new PublicResourceNotFoundException("Public service offering not found"));
	}

	private PublicBarberResponse requireVisibleBarber(PublicTenantContext tenant, Long barberId) {
		return publicBarberShopRepositoryPort.findVisibleBarberByTenant(
				tenant.companyId(),
				tenant.branchId(),
				barberId
		)
				.orElseThrow(() -> new PublicResourceNotFoundException("Public barber not found"));
	}

	private Customer findOrCreateCustomer(Customer candidate) {
		return customerRepositoryPort.findByPhone(candidate.phone())
				.map(customer -> {
					if (!customer.active()) {
						throw new ResourceInactiveException("Customer must be active");
					}
					return customer;
				})
				.orElseGet(() -> customerRepositoryPort.save(candidate));
	}

	private String normalizeSlug(String slug) {
		return slug == null ? null : slug.strip().toLowerCase();
	}
}
