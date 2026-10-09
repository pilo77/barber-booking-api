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

	@Test void issuedTokenIsRevokedAfterAccountSessionVersionChanges() {
		var versions = org.mockito.Mockito.mock(com.villamil.barberbooking.application.port.out.SessionVersionRepositoryPort.class);
		org.mockito.Mockito.when(versions.currentVersionForPasswordSnapshot(1L, "test-hash"))
				.thenReturn(java.util.OptionalLong.of(0));
		org.mockito.Mockito.when(versions.currentVersion(1L)).thenReturn(0L);
		var adapter = new JwtTokenAdapter(VALID_SECRET, 15, "issuer-one", "audience-one", versions);
		String token = adapter.createAccessToken(user());
		assertThat(adapter.parse(token).id()).isEqualTo(1L);
		org.mockito.Mockito.when(versions.currentVersion(1L)).thenReturn(1L);
		assertThatThrownBy(() -> adapter.parse(token)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Session is no longer valid");
	}

	@Test void legacyTokenWithoutVersionOnlyWorksForAccountVersionZero() {
		var versions = org.mockito.Mockito.mock(com.villamil.barberbooking.application.port.out.SessionVersionRepositoryPort.class);
		var adapter = new JwtTokenAdapter(VALID_SECRET, 15, "issuer-one", "audience-one", versions);
		String token = signedToken(null);
		assertThat(adapter.parse(token).id()).isEqualTo(1L);
		org.mockito.Mockito.when(versions.currentVersion(1L)).thenReturn(1L);
		assertThatThrownBy(() -> adapter.parse(token)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test void stalePasswordSnapshotCannotIssueTokenAfterConcurrentPasswordChange() {
		var versions = org.mockito.Mockito.mock(com.villamil.barberbooking.application.port.out.SessionVersionRepositoryPort.class);
		org.mockito.Mockito.when(versions.currentVersionForPasswordSnapshot(1L, "test-hash"))
				.thenReturn(java.util.OptionalLong.empty());
		var adapter = new JwtTokenAdapter(VALID_SECRET, 15, "issuer-one", "audience-one", versions);
		assertThatThrownBy(() -> adapter.createAccessToken(user()))
				.isInstanceOf(com.villamil.barberbooking.domain.exception.AuthenticationFailedException.class)
				.hasMessage("Invalid email or password");
	}

	@Test void replacementTokenCarriesCurrentVersion() {
		var versions = org.mockito.Mockito.mock(com.villamil.barberbooking.application.port.out.SessionVersionRepositoryPort.class);
		org.mockito.Mockito.when(versions.currentVersionForPasswordSnapshot(1L, "test-hash"))
				.thenReturn(java.util.OptionalLong.of(3));
		org.mockito.Mockito.when(versions.currentVersion(1L)).thenReturn(3L);
		var adapter = new JwtTokenAdapter(VALID_SECRET, 15, "issuer-one", "audience-one", versions);
		assertThat(adapter.parse(adapter.createAccessToken(user())).id()).isEqualTo(1L);
	}

	@Test void fractionalSessionVersionIsNotTruncatedAndAccepted() {
		assertThatThrownBy(() -> new JwtTokenAdapter(VALID_SECRET, 15, "issuer-one", "audience-one").parse(signedToken(0.5)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private com.villamil.barberbooking.domain.model.UserAccount user() {
		return new com.villamil.barberbooking.domain.model.UserAccount(1L, 1L, 1L,
				"user@example.com", "test-hash", "User", null, null, true,
				java.time.Instant.EPOCH, java.time.Instant.EPOCH,
				java.util.Set.of(com.villamil.barberbooking.domain.model.Role.COMPANY_OWNER));
	}

	private String signedToken(Number version) {
		var builder = io.jsonwebtoken.Jwts.builder().issuer("issuer-one").audience().add("audience-one").and()
				.id(java.util.UUID.randomUUID().toString()).subject("1")
				.claim("userId", 1L).claim("email", "user@example.com").claim("fullName", "User")
				.claim("companyId", 1L).claim("branchId", 1L).claim("roles", java.util.List.of("COMPANY_OWNER"))
				.expiration(java.util.Date.from(java.time.Instant.now().plusSeconds(300)));
		if (version != null) builder.claim("sessionVersion", version);
		return builder.signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(VALID_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
				.compact();
	}
}
