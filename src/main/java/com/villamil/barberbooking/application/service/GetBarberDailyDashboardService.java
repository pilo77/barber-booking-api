package com.villamil.barberbooking.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.GetBarberDailyDashboardCommand;
import com.villamil.barberbooking.application.dto.response.BarberDailyDashboardResponse;
import com.villamil.barberbooking.application.dto.response.BarberDailyDashboardSummaryResponse;
import com.villamil.barberbooking.application.dto.response.DailyAppointmentItemResponse;
import com.villamil.barberbooking.application.port.in.GetBarberDailyDashboardUseCase;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;
import com.villamil.barberbooking.domain.exception.CustomerNotFoundException;
import com.villamil.barberbooking.domain.exception.ServiceOfferingNotFoundException;
import com.villamil.barberbooking.domain.model.Appointment;
import com.villamil.barberbooking.domain.model.Barber;
import com.villamil.barberbooking.domain.model.Customer;
import com.villamil.barberbooking.domain.model.ServiceOffering;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

@Service
class GetBarberDailyDashboardService implements GetBarberDailyDashboardUseCase {

	private final BarberRepositoryPort barberRepositoryPort;
	private final AppointmentRepositoryPort appointmentRepositoryPort;
	private final CustomerRepositoryPort customerRepositoryPort;
	private final ServiceOfferingRepositoryPort serviceOfferingRepositoryPort;
	private final CurrentUserResolver currentUserResolver;
	private final UserAuthorizationPolicy userAuthorizationPolicy;
	private final Clock clock;

	@Autowired
	GetBarberDailyDashboardService(
			BarberRepositoryPort barberRepositoryPort,
			AppointmentRepositoryPort appointmentRepositoryPort,
			CustomerRepositoryPort customerRepositoryPort,
			ServiceOfferingRepositoryPort serviceOfferingRepositoryPort,
			CurrentUserResolver currentUserResolver,
			UserAuthorizationPolicy userAuthorizationPolicy
	) {
		this(
				barberRepositoryPort,
				appointmentRepositoryPort,
				customerRepositoryPort,
				serviceOfferingRepositoryPort,
				currentUserResolver,
				userAuthorizationPolicy,
				Clock.systemDefaultZone()
		);
	}

	GetBarberDailyDashboardService(
			BarberRepositoryPort barberRepositoryPort,
			AppointmentRepositoryPort appointmentRepositoryPort,
			CustomerRepositoryPort customerRepositoryPort,
			ServiceOfferingRepositoryPort serviceOfferingRepositoryPort,
			Clock clock
	) {
		this(
				barberRepositoryPort,
				appointmentRepositoryPort,
				customerRepositoryPort,
				serviceOfferingRepositoryPort,
				null,
				null,
				clock
		);
	}

	GetBarberDailyDashboardService(
			BarberRepositoryPort barberRepositoryPort,
			AppointmentRepositoryPort appointmentRepositoryPort,
			CustomerRepositoryPort customerRepositoryPort,
			ServiceOfferingRepositoryPort serviceOfferingRepositoryPort,
			CurrentUserResolver currentUserResolver,
			UserAuthorizationPolicy userAuthorizationPolicy,
			Clock clock
	) {
		this.barberRepositoryPort = barberRepositoryPort;
		this.appointmentRepositoryPort = appointmentRepositoryPort;
		this.customerRepositoryPort = customerRepositoryPort;
		this.serviceOfferingRepositoryPort = serviceOfferingRepositoryPort;
		this.currentUserResolver = currentUserResolver;
		this.userAuthorizationPolicy = userAuthorizationPolicy;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public BarberDailyDashboardResponse getDailyDashboard(GetBarberDailyDashboardCommand command) {
		if (currentUserResolver != null) {
			userAuthorizationPolicy.ensureCanAccessBarberSchedule(
					currentUserResolver.requireCurrentUser(),
					command.barberId()
			);
		}
		Barber barber = barberRepositoryPort.findById(command.barberId())
				.orElseThrow(() -> new BarberNotFoundException("Barber not found"));

		List<Appointment> appointments = appointmentRepositoryPort
				.findByBarberIdAndDate(command.barberId(), command.date())
				.stream()
				.sorted(Comparator.comparing(Appointment::startAt))
				.toList();

		Map<Long, Customer> customersById = loadCustomers(appointments);
		Map<Long, ServiceOffering> servicesById = loadServiceOfferings(appointments);

		List<DailyAppointmentItemResponse> items = appointments.stream()
				.map(appointment -> toItem(appointment, customersById, servicesById))
				.toList();

		return new BarberDailyDashboardResponse(
				barber.id(),
				barber.fullName(),
				command.date(),
				buildSummary(appointments),
				findNextAppointment(items).orElse(null),
				items
		);
	}

	private Map<Long, Customer> loadCustomers(List<Appointment> appointments) {
		return appointments.stream()
				.map(Appointment::customerId)
				.distinct()
				.map(customerId -> customerRepositoryPort.findById(customerId)
						.orElseThrow(() -> new CustomerNotFoundException("Customer not found")))
				.collect(Collectors.toMap(Customer::id, Function.identity()));
	}

	private Map<Long, ServiceOffering> loadServiceOfferings(List<Appointment> appointments) {
		return appointments.stream()
				.map(Appointment::serviceOfferingId)
				.distinct()
				.map(serviceOfferingId -> serviceOfferingRepositoryPort.findById(serviceOfferingId)
						.orElseThrow(() -> new ServiceOfferingNotFoundException("Service offering not found")))
				.collect(Collectors.toMap(ServiceOffering::id, Function.identity()));
	}

	private DailyAppointmentItemResponse toItem(
			Appointment appointment,
			Map<Long, Customer> customersById,
			Map<Long, ServiceOffering> servicesById
	) {
		Customer customer = customersById.get(appointment.customerId());
		ServiceOffering serviceOffering = servicesById.get(appointment.serviceOfferingId());
		return new DailyAppointmentItemResponse(
				appointment.id(),
				appointment.customerId(),
				customer.fullName(),
				appointment.serviceOfferingId(),
				serviceOffering.name(),
				appointment.startAt(),
				appointment.endAt(),
				appointment.status(),
				appointment.source()
		);
	}

	private BarberDailyDashboardSummaryResponse buildSummary(List<Appointment> appointments) {
		int scheduled = countByStatus(appointments, AppointmentStatus.SCHEDULED);
		int inProgress = countByStatus(appointments, AppointmentStatus.IN_PROGRESS);
		int completed = countByStatus(appointments, AppointmentStatus.COMPLETED);
		int cancelled = countByStatus(appointments, AppointmentStatus.CANCELLED);
		int noShow = countByStatus(appointments, AppointmentStatus.NO_SHOW);
		long occupiedMinutes = appointments.stream()
				.filter(this::countsAsOccupied)
				.mapToLong(appointment -> Duration.between(appointment.startAt(), appointment.endAt()).toMinutes())
				.sum();

		return new BarberDailyDashboardSummaryResponse(
				appointments.size(),
				scheduled,
				inProgress,
				completed,
				cancelled,
				noShow,
				occupiedMinutes
		);
	}

	private int countByStatus(List<Appointment> appointments, AppointmentStatus status) {
		return (int) appointments.stream()
				.filter(appointment -> appointment.status() == status)
				.count();
	}

	private boolean countsAsOccupied(Appointment appointment) {
		return appointment.status() == AppointmentStatus.SCHEDULED
				|| appointment.status() == AppointmentStatus.IN_PROGRESS
				|| appointment.status() == AppointmentStatus.COMPLETED;
	}

	private Optional<DailyAppointmentItemResponse> findNextAppointment(List<DailyAppointmentItemResponse> items) {
		LocalDateTime now = LocalDateTime.now(clock);
		return items.stream()
				.filter(item -> item.status() == AppointmentStatus.SCHEDULED
						|| item.status() == AppointmentStatus.IN_PROGRESS)
				.filter(item -> item.endAt().isAfter(now))
				.findFirst();
	}
}
