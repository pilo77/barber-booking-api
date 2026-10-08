package com.villamil.barberbooking.infrastructure.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.PasswordHasherPort;

@Component
public class BCryptPasswordHasherAdapter implements PasswordHasherPort {

	private final PasswordEncoder passwordEncoder;
	private final String dummyHash;

	public BCryptPasswordHasherAdapter(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
		this.dummyHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
	}

	@Override
	public String hash(String rawPassword) {
		if (rawPassword == null || rawPassword.length() < 12
				|| rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
			throw new com.villamil.barberbooking.domain.exception.BusinessRuleException(
					"Password must be at least 12 characters and at most 72 UTF-8 bytes");
		}
		return passwordEncoder.encode(rawPassword);
	}

	@Override
	public void mitigateMissingAccount(String rawPassword) {
		matches(rawPassword, dummyHash);
	}

	@Override
	public boolean matches(String rawPassword, String passwordHash) {
		if (rawPassword == null || rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) return false;
		return passwordEncoder.matches(rawPassword, passwordHash);
	}
}
