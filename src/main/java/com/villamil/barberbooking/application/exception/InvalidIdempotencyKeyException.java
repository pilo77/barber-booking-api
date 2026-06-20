package com.villamil.barberbooking.application.exception;

public class InvalidIdempotencyKeyException extends RuntimeException {

	public InvalidIdempotencyKeyException(String message) {
		super(message);
	}
}
