package com.villamil.barberbooking.application.exception;

public class MissingIdempotencyKeyException extends RuntimeException {

	public MissingIdempotencyKeyException(String message) {
		super(message);
	}
}
