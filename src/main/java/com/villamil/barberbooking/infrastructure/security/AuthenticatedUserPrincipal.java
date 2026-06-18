package com.villamil.barberbooking.infrastructure.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;

public class AuthenticatedUserPrincipal implements UserDetails {

	private final AuthenticatedUserResponse user;
	private final Collection<? extends GrantedAuthority> authorities;

	public AuthenticatedUserPrincipal(
			AuthenticatedUserResponse user,
			Collection<? extends GrantedAuthority> authorities
	) {
		this.user = user;
		this.authorities = authorities;
	}

	public AuthenticatedUserResponse user() {
		return user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return "";
	}

	@Override
	public String getUsername() {
		return user.email();
	}
}
