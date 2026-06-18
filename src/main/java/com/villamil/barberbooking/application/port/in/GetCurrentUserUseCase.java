package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;

public interface GetCurrentUserUseCase {

	AuthenticatedUserResponse me();
}
