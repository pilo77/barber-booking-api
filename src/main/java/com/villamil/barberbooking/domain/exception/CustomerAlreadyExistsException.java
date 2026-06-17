package com.villamil.barberbooking.domain.exception;

public class CustomerAlreadyExistsException extends BusinessRuleException {

	public CustomerAlreadyExistsException(String message) {
		super(message);
	}
}
