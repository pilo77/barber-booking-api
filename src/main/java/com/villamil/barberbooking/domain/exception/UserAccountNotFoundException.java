package com.villamil.barberbooking.domain.exception;

public class UserAccountNotFoundException extends RuntimeException {

	public UserAccountNotFoundException(String message) {
		super(message);
	}
}
