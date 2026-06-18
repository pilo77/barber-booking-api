package com.villamil.barberbooking.domain.exception;

public class AuthenticationFailedException extends RuntimeException {

	public AuthenticationFailedException(String message) {
		super(message);
	}
}
