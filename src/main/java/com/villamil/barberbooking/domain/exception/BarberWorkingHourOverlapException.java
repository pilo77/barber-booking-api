package com.villamil.barberbooking.domain.exception;

public class BarberWorkingHourOverlapException extends RuntimeException {

	public BarberWorkingHourOverlapException(String message) {
		super(message);
	}
}
