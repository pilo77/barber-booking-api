package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.BootstrapUserCommand;
import com.villamil.barberbooking.application.dto.response.UserAccountResponse;

public interface BootstrapUserUseCase {

	UserAccountResponse bootstrap(BootstrapUserCommand command);
}
