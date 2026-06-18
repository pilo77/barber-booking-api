package com.villamil.barberbooking.domain.exception;

public class AppointmentInvalidStatusTransitionException extends RuntimeException {

	public AppointmentInvalidStatusTransitionException(String message) {
		super(message);
	}
}
