package com.villamil.barberbooking.domain.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

public record Appointment(
		Long id,
		Long customerId,
		Long barberId,
		Long serviceId,
		LocalDateTime startAt,
		LocalDateTime endAt,
		AppointmentStatus status,
		AppointmentSource source,
		Instant createdAt
) {

	public Appointment {
		validateId(id, "Appointment id");
		customerId = requirePositive(customerId, "Customer id is required");
		barberId = requirePositive(barberId, "Barber id is required");
		serviceId = requirePositive(serviceId, "Service id is required");
		Objects.requireNonNull(startAt, "Appointment start date is required");
		Objects.requireNonNull(endAt, "Appointment end date is required");
		Objects.requireNonNull(status, "Appointment status is required");
		Objects.requireNonNull(source, "Appointment source is required");
		if (!startAt.isBefore(endAt)) {
			throw new BusinessRuleException("Appointment start date must be before end date");
		}
		createdAt = createdAt == null ? Instant.now() : createdAt;
	}

	public static Appointment create(
			Long customerId,
			Long barberId,
			Long serviceId,
			LocalDateTime startAt,
			int durationMinutes,
			AppointmentSource source
	) {
		if (durationMinutes <= 0) {
			throw new BusinessRuleException("Appointment duration must be positive");
		}
		AppointmentSource appointmentSource = source == null ? AppointmentSource.ONLINE : source;
		return new Appointment(
				null,
				customerId,
				barberId,
				serviceId,
				startAt,
				startAt.plusMinutes(durationMinutes),
				AppointmentStatus.SCHEDULED,
				appointmentSource,
				Instant.now()
		);
	}

	public boolean overlaps(LocalDateTime newStartAt, LocalDateTime newEndAt) {
		Objects.requireNonNull(newStartAt, "New start date is required");
		Objects.requireNonNull(newEndAt, "New end date is required");
		return startAt.isBefore(newEndAt) && endAt.isAfter(newStartAt);
	}

	public boolean blocksAvailability() {
		return status.blocksAvailability();
	}

	public Appointment cancel() {
		return changeStatus(AppointmentStatus.CANCELLED);
	}

	public Appointment start() {
		return changeStatus(AppointmentStatus.IN_PROGRESS);
	}

	public Appointment complete() {
		return changeStatus(AppointmentStatus.COMPLETED);
	}

	public Appointment markNoShow() {
		return changeStatus(AppointmentStatus.NO_SHOW);
	}

	private Appointment changeStatus(AppointmentStatus newStatus) {
		return new Appointment(id, customerId, barberId, serviceId, startAt, endAt, newStatus, source, createdAt);
	}

	private static Long requirePositive(Long value, String message) {
		if (value == null || value <= 0) {
			throw new BusinessRuleException(message);
		}
		return value;
	}

	private static void validateId(Long value, String label) {
		if (value != null && value <= 0) {
			throw new BusinessRuleException(label + " must be positive");
		}
	}
}
