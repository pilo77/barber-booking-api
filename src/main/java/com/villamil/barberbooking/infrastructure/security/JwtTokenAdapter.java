package com.villamil.barberbooking.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.port.out.JwtTokenPort;
import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.domain.model.UserAccount;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenAdapter implements JwtTokenPort {

	private final SecretKey signingKey;
	private final long expirationMinutes;
	private final String issuer;
	private final String audience;

	static final String KNOWN_INSECURE_DEFAULT = "local-dev-only-change-this-secret-with-at-least-32-chars";
	static final int MIN_SECRET_LENGTH = 32;

	public JwtTokenAdapter(
			String secret, long expirationMinutes
	) {
		this(secret, expirationMinutes, "barberia-ghs", "barberia-ghs-web");
	}

	@Autowired
	public JwtTokenAdapter(
			@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-minutes:60}") long expirationMinutes,
			@Value("${app.jwt.issuer:barberia-ghs}") String issuer,
			@Value("${app.jwt.audience:barberia-ghs-web}") String audience
	) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("app.jwt.secret must not be null or blank");
		}
		if (secret.length() < MIN_SECRET_LENGTH) {
			throw new IllegalStateException(
					"app.jwt.secret must be at least " + MIN_SECRET_LENGTH + " characters");
		}
		if (KNOWN_INSECURE_DEFAULT.equals(secret)) {
			throw new IllegalStateException(
					"app.jwt.secret is set to the known insecure default value; configure a unique secret");
		}
		if (secret.startsWith("<") || secret.contains("placeholder")) {
			throw new IllegalStateException("app.jwt.secret must not be a placeholder");
		}
		if (expirationMinutes <= 0 || expirationMinutes > 60 || issuer.isBlank() || audience.isBlank()) {
			throw new IllegalStateException("JWT requires issuer, audience and expiration between 1 and 60 minutes");
		}
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMinutes = expirationMinutes;
		this.issuer = issuer;
		this.audience = audience;
	}

	@Override
	public String createAccessToken(UserAccount userAccount) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(expiresInSeconds());
		return Jwts.builder()
				.issuer(issuer)
				.audience().add(audience).and()
				.id(UUID.randomUUID().toString())
				.subject(String.valueOf(userAccount.id()))
				.claim("userId", userAccount.id())
				.claim("email", userAccount.email())
				.claim("fullName", userAccount.fullName())
				.claim("companyId", userAccount.companyId())
				.claim("branchId", userAccount.branchId())
				.claim("barberId", userAccount.barberId())
				.claim("roles", userAccount.roles().stream().map(Role::name).toList())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.signWith(signingKey)
				.compact();
	}

	@Override
	public long expiresInSeconds() {
		return expirationMinutes * 60;
	}

	@Override
	public AuthenticatedUserResponse parse(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(signingKey)
				.requireIssuer(issuer)
				.requireAudience(audience)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		Long userId = claims.get("userId", Number.class).longValue();
		if (userId <= 0 || !String.valueOf(userId).equals(claims.getSubject())
				|| claims.getId() == null || claims.getId().isBlank() || claims.getExpiration() == null) {
			throw new IllegalArgumentException("Invalid access token claims");
		}
		Long companyId = numberClaim(claims, "companyId");
		Long branchId = numberClaim(claims, "branchId");
		Long barberId = numberClaim(claims, "barberId");
		@SuppressWarnings("unchecked")
		List<String> rawRoles = claims.get("roles", List.class);
		Set<Role> roles = rawRoles.stream()
				.map(Role::valueOf)
				.collect(Collectors.toUnmodifiableSet());
		return new AuthenticatedUserResponse(
				userId,
				claims.get("email", String.class),
				claims.get("fullName", String.class),
				companyId,
				branchId,
				barberId,
				roles
		);
	}

	private Long numberClaim(Claims claims, String claimName) {
		Number value = claims.get(claimName, Number.class);
		return value == null ? null : value.longValue();
	}
}
