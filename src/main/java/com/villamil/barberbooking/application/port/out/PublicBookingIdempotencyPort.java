package com.villamil.barberbooking.application.port.out;

import java.util.Optional;

import com.villamil.barberbooking.application.idempotency.PublicBookingIdempotencyRecord;

public interface PublicBookingIdempotencyPort {

	String tryStart(String idempotencyKey, String requestHash);

	Optional<PublicBookingIdempotencyRecord> find(String idempotencyKey);

	boolean lockCurrentClaimForSideEffect(String idempotencyKey, String requestHash, String claimToken);

	void complete(String idempotencyKey, String claimToken, Long appointmentId);
}
