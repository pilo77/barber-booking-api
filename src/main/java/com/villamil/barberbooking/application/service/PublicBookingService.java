package com.villamil.barberbooking.application.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.CreatePublicAppointmentCommand;
import com.villamil.barberbooking.application.dto.command.GetBarberAvailabilityCommand;
import com.villamil.barberbooking.application.dto.command.PublicCustomerCommand;
import com.villamil.barberbooking.application.dto.response.BarberAvailabilityResponse;
import com.villamil.barberbooking.application.dto.response.PublicAppointmentResponse;
import com.villamil.barberbooking.application.dto.response.PublicBarberResponse;
import com.villamil.barberbooking.application.dto.response.PublicServiceOfferingResponse;
import com.villamil.barberbooking.application.port.in.CreatePublicAppointmentUseCase;
import com.villamil.barberbooking.application.port.in.GetPublicBarberAvailabilityUseCase;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.application.port.out.PublicBarberShopRepositoryPort;
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

	private final PublicBarberShopRepositoryPort publicBarberShopRepositoryPort;
	private final TenantContextExecutor tenantContextExecutor;
	private final BarberAvailabilityCalculator barberAvailabilityCalculator;
	private final CustomerRepositoryPort customerRepositoryPort;
	private final AppointmentBookingPolicy appointmentBookingPolicy;
	private final AppointmentRepositoryPort appointmentRepositoryPort;

	PublicBookingService(
			PublicBarberShopRepositoryPort publicBarberShopRepositoryPort,
			TenantContextExecutor tenantContextExecutor,
			BarberAvailabilityCalculator barberAvailabilityCalculator,
			CustomerRepositoryPort customerRepositoryPort,
			AppointmentBookingPolicy appointmentBookingPolicy,
			AppointmentRepositoryPort appointmentRepositoryPort
	) {
		this.publicBarberShopRepositoryPort = publicBarberShopRepositoryPort;
		this.tenantContextExecutor = tenantContextExecutor;
		this.barberAvailabilityCalculator = barberAvailabilityCalculator;
		this.customerRepositoryPort = customerRepositoryPort;
		this.appointmentBookingPolicy = appointmentBookingPolicy;
		this.appointmentRepositoryPort = appointmentRepositoryPort;
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
		PublicTenantContext tenant = resolveTenant(command.companySlug(), command.branchSlug());
		PublicServiceOfferingResponse service = requireVisibleService(tenant, command.serviceOfferingId());
		PublicBarberResponse barber = requireVisibleBarber(tenant, command.barberId());

		return tenantContextExecutor.withTenant(tenant.toTenantContext(), () -> {
			Customer customer = findOrCreateCustomer(command.customer());
			Appointment appointment = appointmentBookingPolicy.createValidatedAppointment(
					customer.id(),
					command.barberId(),
					command.serviceOfferingId(),
					command.startAt(),
					AppointmentSource.ONLINE,
					AppointmentStatus.SCHEDULED
			);
			return PublicAppointmentResponse.from(
					appointmentRepositoryPort.save(appointment),
					service,
					barber,
					customer
			);
		});
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

	private Customer findOrCreateCustomer(PublicCustomerCommand command) {
		Customer candidate = Customer.create(command.fullName(), command.phone(), command.email());
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
