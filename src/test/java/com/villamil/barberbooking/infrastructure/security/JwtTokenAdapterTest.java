package com.villamil.barberbooking.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtTokenAdapterTest {

	private static final String VALID_SECRET =
			"this-is-a-safe-test-secret-with-enough-length-123";

	@Test
	void rejectsNullSecret() {
		assertThatThrownBy(() -> new JwtTokenAdapter(null, 60))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("null or blank");
	}

	@Test
	void rejectsBlankSecret() {
		assertThatThrownBy(() -> new JwtTokenAdapter("   ", 60))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("null or blank");
	}

	@Test
	void rejectsSecretShorterThan32Chars() {
		assertThatThrownBy(() -> new JwtTokenAdapter("too-short", 60))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("at least 32 characters");
	}

	@Test
	void rejectsKnownInsecureDefault() {
		assertThatThrownBy(() -> new JwtTokenAdapter(JwtTokenAdapter.KNOWN_INSECURE_DEFAULT, 60))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("known insecure default");
	}

	@Test
	void acceptsValidSecret() {
		JwtTokenAdapter adapter = new JwtTokenAdapter(VALID_SECRET, 60);
		assertThat(adapter).isNotNull();
	}
}
