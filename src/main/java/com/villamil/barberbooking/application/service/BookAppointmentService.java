package com.villamil.barberbooking.application.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.BookAppointmentCommand;
import com.villamil.barberbooking.application.dto.response.AppointmentResponse;
import com.villamil.barberbooking.application.port.in.BookAppointmentUseCase;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;
import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;
import com.villamil.barberbooking.domain.exception.AppointmentNotAvailableException;
import com.villamil.barberbooking.domain.exception.AppointmentOutsideWorkingHoursException;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.exception.CustomerNotFoundException;
import com.villamil.barberbooking.domain.exception.ServiceOfferingNotFoundException;
import com.villamil.barberbooking.domain.model.Appointment;
import com.villamil.barberbooking.domain.model.Barber;
import com.villamil.barberbooking.domain.model.Customer;
import com.villamil.barberbooking.domain.model.ServiceOffering;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;

@Service
class BookAppointmentService implements BookAppointmentUseCase {

	private final AppointmentRepositoryPort appointmentRepositoryPort;
	private final CustomerRepositoryPort customerRepositoryPort;
	private final BarberRepositoryPort barberRepositoryPort;
	private final ServiceOfferingRepositoryPort serviceOfferingRepositoryPort;
	private final BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort;

	BookAppointmentService(
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

	@Override
	@Transactional
	public AppointmentResponse book(BookAppointmentCommand command) {
		Customer customer = customerRepositoryPort.findById(command.customerId())
				.orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
		if (!customer.active()) {
			throw new BusinessRuleException("Customer must be active");
		}

		Barber barber = barberRepositoryPort.findById(command.barberId())
				.orElseThrow(() -> new BarberNotFoundException("Barber not found"));
		if (!barber.active()) {
			throw new BusinessRuleException("Barber must be active");
		}

		ServiceOffering serviceOffering = serviceOfferingRepositoryPort.findById(command.serviceOfferingId())
				.orElseThrow(() -> new ServiceOfferingNotFoundException("Service offering not found"));
		if (!serviceOffering.active()) {
			throw new BusinessRuleException("Service offering must be active");
		}

		Appointment appointment = Appointment.create(
				customer.id(),
				barber.id(),
				serviceOffering.id(),
				command.startAt(),
				serviceOffering.durationMinutes(),
				AppointmentSource.ONLINE
		);

		ensureWithinWorkingHours(appointment);
		ensureNoBlockingOverlap(appointment);

		return AppointmentResponse.from(appointmentRepositoryPort.save(appointment));
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
