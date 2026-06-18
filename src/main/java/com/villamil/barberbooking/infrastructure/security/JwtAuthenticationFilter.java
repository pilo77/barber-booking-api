package com.villamil.barberbooking.infrastructure.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.port.out.JwtTokenPort;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER = "Bearer ";

	private final JwtTokenPort jwtTokenPort;

	public JwtAuthenticationFilter(JwtTokenPort jwtTokenPort) {
		this.jwtTokenPort = jwtTokenPort;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String authorization = request.getHeader(AUTHORIZATION);
		if (authorization != null && authorization.startsWith(BEARER)) {
			try {
				AuthenticatedUserResponse user = jwtTokenPort.parse(authorization.substring(BEARER.length()));
				AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
						user,
						user.roles().stream()
								.map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
								.toList()
				);
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						principal,
						null,
						principal.getAuthorities()
				);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
			catch (RuntimeException exception) {
				SecurityContextHolder.clearContext();
			}
		}
		filterChain.doFilter(request, response);
	}
}
