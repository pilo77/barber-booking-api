package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.response.UserAccountResponse;

public interface ActivateUserAccountUseCase {

	UserAccountResponse activate(Long id);
}
