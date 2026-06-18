package com.villamil.barberbooking.application.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;
import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;
import com.villamil.barberbooking.domain.exception.AppointmentNotAvailableException;
import com.villamil.barberbooking.domain.exception.AppointmentOutsideWorkingHoursException;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;
import com.villamil.barberbooking.domain.exception.CustomerNotFoundException;
import com.villamil.barberbooking.domain.exception.ResourceInactiveException;
import com.villamil.barberbooking.domain.exception.ServiceOfferingNotFoundException;
import com.villamil.barberbooking.domain.model.Appointment;
import com.villamil.barberbooking.domain.model.Barber;
import com.villamil.barberbooking.domain.model.Customer;
import com.villamil.barberbooking.domain.model.ServiceOffering;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

@Component
class AppointmentBookingPolicy {

	private final AppointmentRepositoryPort appointmentRepositoryPort;
	private final CustomerRepositoryPort customerRepositoryPort;
	private final BarberRepositoryPort barberRepositoryPort;
	private final ServiceOfferingRepositoryPort serviceOfferingRepositoryPort;
	private final BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort;

	AppointmentBookingPolicy(
			AppointmentRepositoryPort appointmentRepositoryPort,
			CustomerRepositoryPort customerRepositoryPort,
			BarberRepositoryPort barberRepositoryPort,
			ServiceOfferingRepositoryPort serviceOfferingRepositoryPort,
			BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort
	) {
		this.appointmentRepositoryPort = appointmentRepositoryPort;
		this.customerRepositoryPort = customerRepositoryPort;
		this.barberRepositoryPort = barberRepositoryPort;
		this.serviceOfferingRepositoryPort = serviceOfferingRepositoryPort;
		this.barberWorkingHourRepositoryPort = barberWorkingHourRepositoryPort;
	}

	Appointment createValidatedAppointment(
			Long customerId,
			Long barberId,
			Long serviceOfferingId,
			LocalDateTime startAt,
			AppointmentSource source,
			AppointmentStatus initialStatus
	) {
		Customer customer = customerRepositoryPort.findById(customerId)
				.orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
		if (!customer.active()) {
			throw new ResourceInactiveException("Customer must be active");
		}

		Barber barber = barberRepositoryPort.findById(barberId)
				.orElseThrow(() -> new BarberNotFoundException("Barber not found"));
		if (!barber.active()) {
			throw new ResourceInactiveException("Barber must be active");
		}

		ServiceOffering serviceOffering = serviceOfferingRepositoryPort.findById(serviceOfferingId)
				.orElseThrow(() -> new ServiceOfferingNotFoundException("Service offering not found"));
		if (!serviceOffering.active()) {
			throw new ResourceInactiveException("Service offering must be active");
		}

		Appointment appointment = Appointment.create(
				customer.id(),
				barber.id(),
				serviceOffering.id(),
				startAt,
				serviceOffering.durationMinutes(),
				source,
				initialStatus
		);

		ensureWithinWorkingHours(appointment);
		ensureNoBlockingOverlap(appointment);

		return appointment;
	}

	private void ensureWithinWorkingHours(Appointment appointment) {
		LocalDateTime startAt = appointment.startAt();
		LocalDateTime endAt = appointment.endAt();
		if (!startAt.toLocalDate().equals(endAt.toLocalDate())) {
			throw new AppointmentOutsideWorkingHoursException("Appointment is outside barber working hours");
		}

		boolean insideWorkingHours = barberWorkingHourRepositoryPort
				.findActiveByBarberIdAndDay(appointment.barberId(), startAt.getDayOfWeek())
				.stream()
				.anyMatch(workingHour ->
						!startAt.toLocalTime().isBefore(workingHour.startTime())
								&& !endAt.toLocalTime().isAfter(workingHour.endTime())
				);

		if (!insideWorkingHours) {
			throw new AppointmentOutsideWorkingHoursException("Appointment is outside barber working hours");
		}
	}

	private void ensureNoBlockingOverlap(Appointment appointment) {
		if (appointmentRepositoryPort.existsBlockingOverlap(
				appointment.barberId(),
				appointment.startAt(),
				appointment.endAt()
		)) {
			throw new AppointmentNotAvailableException("Appointment overlaps with an active appointment");
		}
	}
}
