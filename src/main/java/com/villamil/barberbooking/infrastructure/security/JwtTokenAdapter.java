package com.villamil.barberbooking.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

	public JwtTokenAdapter(
			@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-minutes:60}") long expirationMinutes
	) {
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMinutes = expirationMinutes;
	}

	@Override
	public String createAccessToken(UserAccount userAccount) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(expiresInSeconds());
		return Jwts.builder()
				.subject(String.valueOf(userAccount.id()))
				.claim("userId", userAccount.id())
				.claim("email", userAccount.email())
				.claim("fullName", userAccount.fullName())
				.claim("companyId", userAccount.companyId())
				.claim("branchId", userAccount.branchId())
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
				.build()
				.parseSignedClaims(token)
				.getPayload();
		Long userId = claims.get("userId", Number.class).longValue();
		Long companyId = numberClaim(claims, "companyId");
		Long branchId = numberClaim(claims, "branchId");
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
				roles
		);
	}

	private Long numberClaim(Claims claims, String claimName) {
		Number value = claims.get(claimName, Number.class);
		return value == null ? null : value.longValue();
	}
}
