package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.ChangePasswordCommand;

public interface PasswordSecurityUseCase {
    void changePassword(ChangePasswordCommand command);
}
