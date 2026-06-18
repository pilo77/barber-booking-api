package com.villamil.barberbooking.domain.exception;

public class UserAccountAlreadyExistsException extends RuntimeException {

	public UserAccountAlreadyExistsException(String message) {
		super(message);
	}
}
