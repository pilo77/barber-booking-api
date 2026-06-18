package com.villamil.barberbooking.application.port.out;

import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.domain.model.UserAccount;

public interface JwtTokenPort {

	String createAccessToken(UserAccount userAccount);

	long expiresInSeconds();

	AuthenticatedUserResponse parse(String token);
}
