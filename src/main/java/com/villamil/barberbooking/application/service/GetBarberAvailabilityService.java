package com.villamil.barberbooking.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.GetBarberAvailabilityCommand;
import com.villamil.barberbooking.application.dto.response.AvailabilitySlotResponse;
import com.villamil.barberbooking.application.dto.response.BarberAvailabilityResponse;
import com.villamil.barberbooking.application.port.in.GetBarberAvailabilityUseCase;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;
import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;
import com.villamil.barberbooking.domain.exception.ResourceInactiveException;
import com.villamil.barberbooking.domain.exception.ServiceOfferingNotFoundException;
import com.villamil.barberbooking.domain.model.Appointment;
import com.villamil.barberbooking.domain.model.Barber;
import com.villamil.barberbooking.domain.model.BarberWorkingHour;
import com.villamil.barberbooking.domain.model.ServiceOffering;

@Service
class GetBarberAvailabilityService implements GetBarberAvailabilityUseCase {

	private final BarberRepositoryPort barberRepositoryPort;
	private final ServiceOfferingRepositoryPort serviceOfferingRepositoryPort;
	private final BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort;
	private final AppointmentRepositoryPort appointmentRepositoryPort;
	private final CurrentUserResolver currentUserResolver;
	private final UserAuthorizationPolicy userAuthorizationPolicy;
	private final int slotStepMinutes;

	@Autowired
	GetBarberAvailabilityService(
			BarberRepositoryPort barberRepositoryPort,
			ServiceOfferingRepositoryPort serviceOfferingRepositoryPort,
			BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort,
			AppointmentRepositoryPort appointmentRepositoryPort,
			CurrentUserResolver currentUserResolver,
			UserAuthorizationPolicy userAuthorizationPolicy,
			@Value("${booking.slot-step-minutes:15}") int slotStepMinutes
	) {
		if (slotStepMinutes <= 0) {
			throw new IllegalArgumentException("Booking slot step minutes must be positive");
		}
		this.barberRepositoryPort = barberRepositoryPort;
		this.serviceOfferingRepositoryPort = serviceOfferingRepositoryPort;
		this.barberWorkingHourRepositoryPort = barberWorkingHourRepositoryPort;
		this.appointmentRepositoryPort = appointmentRepositoryPort;
		this.currentUserResolver = currentUserResolver;
		this.userAuthorizationPolicy = userAuthorizationPolicy;
		this.slotStepMinutes = slotStepMinutes;
	}

	GetBarberAvailabilityService(
			BarberRepositoryPort barberRepositoryPort,
			ServiceOfferingRepositoryPort serviceOfferingRepositoryPort,
			BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort,
			AppointmentRepositoryPort appointmentRepositoryPort,
			int slotStepMinutes
	) {
		this(
				barberRepositoryPort,
				serviceOfferingRepositoryPort,
				barberWorkingHourRepositoryPort,
				appointmentRepositoryPort,
				null,
				null,
				slotStepMinutes
		);
	}

	@Override
	@Transactional(readOnly = true)
	public BarberAvailabilityResponse getAvailability(GetBarberAvailabilityCommand command) {
		if (currentUserResolver != null) {
			userAuthorizationPolicy.ensureCanAccessBarberSchedule(
					currentUserResolver.requireCurrentUser(),
					command.barberId()
			);
		}
		Barber barber = barberRepositoryPort.findById(command.barberId())
				.orElseThrow(() -> new BarberNotFoundException("Barber not found"));
		if (!barber.active()) {
			throw new ResourceInactiveException("Barber must be active");
		}

		ServiceOffering serviceOffering = serviceOfferingRepositoryPort.findById(command.serviceOfferingId())
				.orElseThrow(() -> new ServiceOfferingNotFoundException("Service offering not found"));
		if (!serviceOffering.active()) {
			throw new ResourceInactiveException("Service offering must be active");
		}

		List<BarberWorkingHour> workingHours = barberWorkingHourRepositoryPort
				.findActiveByBarberIdAndDay(command.barberId(), command.date().getDayOfWeek())
				.stream()
				.sorted(Comparator.comparing(BarberWorkingHour::startTime))
				.toList();

		if (workingHours.isEmpty()) {
			return new BarberAvailabilityResponse(
					command.barberId(),
					command.serviceOfferingId(),
					command.date(),
					List.of()
			);
		}

		List<Appointment> appointments = appointmentRepositoryPort.findByBarberIdAndDate(
				command.barberId(),
				command.date()
		);

		return new BarberAvailabilityResponse(
				command.barberId(),
				command.serviceOfferingId(),
				command.date(),
				buildSlots(command.date(), serviceOffering.durationMinutes(), workingHours, appointments)
		);
	}

	private List<AvailabilitySlotResponse> buildSlots(
			LocalDate date,
			int durationMinutes,
			List<BarberWorkingHour> workingHours,
			List<Appointment> appointments
	) {
		return workingHours.stream()
				.flatMap(workingHour -> buildSlotsForWorkingHour(date, durationMinutes, workingHour, appointments).stream())
				.toList();
	}

	private List<AvailabilitySlotResponse> buildSlotsForWorkingHour(
			LocalDate date,
			int durationMinutes,
			BarberWorkingHour workingHour,
			List<Appointment> appointments
	) {
		List<AvailabilitySlotResponse> slots = new ArrayList<>();
		LocalDateTime slotStart = LocalDateTime.of(date, workingHour.startTime());
		LocalDateTime workingEnd = LocalDateTime.of(date, workingHour.endTime());

		while (!slotStart.plusMinutes(durationMinutes).isAfter(workingEnd)) {
			LocalDateTime slotEnd = slotStart.plusMinutes(durationMinutes);
			slots.add(buildSlot(slotStart, slotEnd, appointments));
			slotStart = slotStart.plusMinutes(slotStepMinutes);
		}

		return slots;
	}

	private AvailabilitySlotResponse buildSlot(
			LocalDateTime slotStart,
			LocalDateTime slotEnd,
			List<Appointment> appointments
	) {
		boolean occupied = appointments.stream()
				.filter(Appointment::blocksAvailability)
				.anyMatch(appointment -> appointment.overlaps(slotStart, slotEnd));

		if (occupied) {
			return AvailabilitySlotResponse.occupied(slotStart, slotEnd);
		}
		return AvailabilitySlotResponse.available(slotStart, slotEnd);
	}
}
