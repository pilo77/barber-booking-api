package com.villamil.barberbooking.domain.exception;

public class CustomerNotFoundException extends BusinessRuleException {

	public CustomerNotFoundException(String message) {
		super(message);
	}
}
