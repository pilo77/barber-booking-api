package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.response.UserAccountResponse;

public interface DeactivateUserAccountUseCase {

	UserAccountResponse deactivate(Long id);
}
