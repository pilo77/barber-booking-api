package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.LoginCommand;
import com.villamil.barberbooking.application.dto.response.LoginResponse;

public interface LoginUseCase {

	LoginResponse login(LoginCommand command);
}
