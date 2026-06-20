package com.villamil.barberbooking.application.port.out;

import java.util.Optional;

import com.villamil.barberbooking.application.idempotency.PublicBookingIdempotencyRecord;

public interface PublicBookingIdempotencyPort {

	boolean tryStart(String idempotencyKey, String requestHash);

	Optional<PublicBookingIdempotencyRecord> find(String idempotencyKey);

	void complete(String idempotencyKey, Long appointmentId);
}
