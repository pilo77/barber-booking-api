package com.villamil.barberbooking.domain.exception;

public class AppointmentOutsideWorkingHoursException extends BusinessRuleException {

	public AppointmentOutsideWorkingHoursException(String message) {
		super(message);
	}
}
