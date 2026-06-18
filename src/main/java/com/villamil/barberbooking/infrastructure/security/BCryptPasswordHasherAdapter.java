package com.villamil.barberbooking.infrastructure.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.PasswordHasherPort;

@Component
public class BCryptPasswordHasherAdapter implements PasswordHasherPort {

	private final PasswordEncoder passwordEncoder;

	public BCryptPasswordHasherAdapter(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public String hash(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}

	@Override
	public boolean matches(String rawPassword, String passwordHash) {
		return passwordEncoder.matches(rawPassword, passwordHash);
	}
}
