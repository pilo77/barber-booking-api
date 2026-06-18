package com.villamil.barberbooking.domain.exception;

public class AppointmentNotFoundException extends RuntimeException {

	public AppointmentNotFoundException(String message) {
		super(message);
	}
}
