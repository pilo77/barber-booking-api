package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.CreateUserAccountCommand;
import com.villamil.barberbooking.application.dto.response.UserAccountResponse;

public interface CreateUserAccountUseCase {

	UserAccountResponse create(CreateUserAccountCommand command);
}
