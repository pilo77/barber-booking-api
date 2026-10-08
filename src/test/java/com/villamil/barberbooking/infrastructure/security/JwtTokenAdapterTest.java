package com.villamil.barberbooking.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtTokenAdapterTest {

	@Test
	void tokenRoundTripRequiresCorrectIssuerAndAudience() {
		var user = new com.villamil.barberbooking.domain.model.UserAccount(1L, 1L, 1L,
				"user@example.com", "test-hash", "User", null, null, true,
				java.time.Instant.EPOCH, java.time.Instant.EPOCH,
				java.util.Set.of(com.villamil.barberbooking.domain.model.Role.COMPANY_OWNER));
		var adapter = new JwtTokenAdapter(VALID_SECRET, 15, "issuer-one", "audience-one");
		String token = adapter.createAccessToken(user);
		assertThat(adapter.parse(token)).isEqualTo(
				com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse.from(user));
		assertThatThrownBy(() -> new JwtTokenAdapter(VALID_SECRET, 15, "issuer-two", "audience-one").parse(token))
				.isInstanceOf(io.jsonwebtoken.IncorrectClaimException.class);
		assertThatThrownBy(() -> new JwtTokenAdapter(VALID_SECRET, 15, "issuer-one", "audience-two").parse(token))
				.isInstanceOf(io.jsonwebtoken.IncorrectClaimException.class);
	}

	@Test
	void rejectsInvalidExpirationAndPlaceholders() {
		assertThatThrownBy(() -> new JwtTokenAdapter(VALID_SECRET, 0)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new JwtTokenAdapter(VALID_SECRET, 61)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new JwtTokenAdapter("<set-local-dev-secret-with-at-least-32-chars>", 60))
				.isInstanceOf(IllegalStateException.class);
	}

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
