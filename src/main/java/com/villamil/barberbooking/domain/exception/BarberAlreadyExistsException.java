package com.villamil.barberbooking.domain.exception;

public class BarberAlreadyExistsException extends BusinessRuleException {

	public BarberAlreadyExistsException(String message) {
		super(message);
	}
}
