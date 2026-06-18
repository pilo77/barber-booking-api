package com.villamil.barberbooking.application.port.out;

import java.util.Optional;

import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;

public interface CurrentUserProvider {

	Optional<AuthenticatedUserResponse> currentUser();
}
