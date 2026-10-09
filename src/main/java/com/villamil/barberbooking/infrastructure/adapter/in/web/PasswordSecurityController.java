package com.villamil.barberbooking.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.villamil.barberbooking.application.dto.command.ChangePasswordCommand;
import com.villamil.barberbooking.application.port.in.PasswordSecurityUseCase;

@RestController
public class PasswordSecurityController {
    private final PasswordSecurityUseCase passwords;

    public PasswordSecurityController(PasswordSecurityUseCase passwords) {
        this.passwords = passwords;
    }

    @PostMapping("/api/v1/auth/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        passwords.changePassword(new ChangePasswordCommand(request.oldPassword(), request.newPassword()));
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Password change data is invalid")
            @Size(max = 72, message = "Password change data is invalid") String oldPassword,
            @NotBlank(message = "Password change data is invalid")
            @Size(min = 12, max = 72, message = "Password change data is invalid") String newPassword
    ) {
        @Override
        public String toString() {
            return "ChangePasswordRequest[redacted]";
        }
    }
}
