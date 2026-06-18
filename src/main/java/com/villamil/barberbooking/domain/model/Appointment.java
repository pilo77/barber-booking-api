package com.villamil.barberbooking.domain.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

import com.villamil.barberbooking.domain.exception.AppointmentInvalidStatusTransitionException;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

public record Appointment(
		Long id,
		Long customerId,
		Long barberId,
		Long serviceOfferingId,
		LocalDateTime startAt,
		LocalDateTime endAt,
		AppointmentStatus status,
		AppointmentSource source,
		Instant createdAt,
		Instant updatedAt
) {

	public Appointment {
		validateId(id, "Appointment id");
		customerId = requirePositive(customerId, "Customer id is required");
		barberId = requirePositive(barberId, "Barber id is required");
		serviceOfferingId = requirePositive(serviceOfferingId, "Service offering id is required");
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
			Long serviceOfferingId,
			LocalDateTime startAt,
			int durationMinutes,
			AppointmentSource source
	) {
		return create(
				customerId,
				barberId,
				serviceOfferingId,
				startAt,
				durationMinutes,
				source,
				AppointmentStatus.SCHEDULED
		);
	}

	public static Appointment create(
			Long customerId,
			Long barberId,
			Long serviceOfferingId,
			LocalDateTime startAt,
			int durationMinutes,
			AppointmentSource source,
			AppointmentStatus initialStatus
	) {
		if (durationMinutes <= 0) {
			throw new BusinessRuleException("Appointment duration must be positive");
		}
		Objects.requireNonNull(startAt, "Appointment start date is required");
		AppointmentSource appointmentSource = source == null ? AppointmentSource.ONLINE : source;
		AppointmentStatus appointmentStatus = Objects.requireNonNull(initialStatus, "Appointment status is required");
		if (appointmentStatus != AppointmentStatus.SCHEDULED && appointmentStatus != AppointmentStatus.IN_PROGRESS) {
			throw new BusinessRuleException("Appointment initial status must be scheduled or in-progress");
		}
		return new Appointment(
				null,
				customerId,
				barberId,
				serviceOfferingId,
				startAt,
				startAt.plusMinutes(durationMinutes),
				appointmentStatus,
				appointmentSource,
				Instant.now(),
				null
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
		ensureStatus(AppointmentStatus.SCHEDULED, "Only scheduled appointments can be cancelled");
		return changeStatus(AppointmentStatus.CANCELLED);
	}

	public Appointment start() {
		ensureStatus(AppointmentStatus.SCHEDULED, "Only scheduled appointments can be started");
		return changeStatus(AppointmentStatus.IN_PROGRESS);
	}

	public Appointment complete() {
		ensureStatus(AppointmentStatus.IN_PROGRESS, "Only in-progress appointments can be completed");
		return changeStatus(AppointmentStatus.COMPLETED);
	}

	public Appointment markNoShow() {
		ensureStatus(AppointmentStatus.SCHEDULED, "Only scheduled appointments can be marked as no-show");
		return changeStatus(AppointmentStatus.NO_SHOW);
	}

	private void ensureStatus(AppointmentStatus expectedStatus, String message) {
		if (status != expectedStatus) {
			throw new AppointmentInvalidStatusTransitionException(message);
		}
	}

	private Appointment changeStatus(AppointmentStatus newStatus) {
		return new Appointment(
				id,
				customerId,
				barberId,
				serviceOfferingId,
				startAt,
				endAt,
				newStatus,
				source,
				createdAt,
				Instant.now()
		);
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
