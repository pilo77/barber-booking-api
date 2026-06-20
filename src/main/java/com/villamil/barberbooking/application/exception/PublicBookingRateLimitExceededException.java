package com.villamil.barberbooking.application.exception;

public class PublicBookingRateLimitExceededException extends RuntimeException {

	public PublicBookingRateLimitExceededException(String message) {
		super(message);
	}
}
