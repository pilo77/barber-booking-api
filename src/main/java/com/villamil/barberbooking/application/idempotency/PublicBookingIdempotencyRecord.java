package com.villamil.barberbooking.application.idempotency;

public record PublicBookingIdempotencyRecord(
		String requestHash,
		Long appointmentId,
		Status status
) {

	public enum Status {
		IN_PROGRESS,
		COMPLETED
	}
}
