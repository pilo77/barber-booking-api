package com.villamil.barberbooking.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.dto.command.GetBarberAvailabilityCommand;
import com.villamil.barberbooking.application.dto.response.AvailabilitySlotResponse;
import com.villamil.barberbooking.application.dto.response.BarberAvailabilityResponse;
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

@Component
class BarberAvailabilityCalculator {

	private final BarberRepositoryPort barberRepositoryPort;
	private final ServiceOfferingRepositoryPort serviceOfferingRepositoryPort;
	private final BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort;
	private final AppointmentRepositoryPort appointmentRepositoryPort;
	private final int slotStepMinutes;

	BarberAvailabilityCalculator(
			BarberRepositoryPort barberRepositoryPort,
			ServiceOfferingRepositoryPort serviceOfferingRepositoryPort,
			BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort,
			AppointmentRepositoryPort appointmentRepositoryPort,
			@Value("${booking.slot-step-minutes:15}") int slotStepMinutes
	) {
		if (slotStepMinutes <= 0) {
			throw new IllegalArgumentException("Booking slot step minutes must be positive");
		}
		this.barberRepositoryPort = barberRepositoryPort;
		this.serviceOfferingRepositoryPort = serviceOfferingRepositoryPort;
		this.barberWorkingHourRepositoryPort = barberWorkingHourRepositoryPort;
		this.appointmentRepositoryPort = appointmentRepositoryPort;
		this.slotStepMinutes = slotStepMinutes;
	}

	BarberAvailabilityResponse calculate(GetBarberAvailabilityCommand command) {
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
			return new BarberAvailabilityResponse(command.barberId(), command.serviceOfferingId(), command.date(), List.of());
		}

		List<Appointment> appointments = appointmentRepositoryPort.findByBarberIdAndDate(command.barberId(), command.date());
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
			boolean occupied = isOccupied(slotStart, slotEnd, appointments);
			slots.add(occupied
					? AvailabilitySlotResponse.occupied(slotStart, slotEnd)
					: AvailabilitySlotResponse.available(slotStart, slotEnd));
			slotStart = slotStart.plusMinutes(slotStepMinutes);
		}

		return slots;
	}

	private boolean isOccupied(
			LocalDateTime slotStart,
			LocalDateTime slotEnd,
			List<Appointment> appointments
	) {
		return appointments.stream()
				.filter(Appointment::blocksAvailability)
				.anyMatch(appointment -> appointment.overlaps(slotStart, slotEnd));
	}
}
