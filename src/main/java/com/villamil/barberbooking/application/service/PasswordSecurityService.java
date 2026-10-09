package com.villamil.barberbooking.application.service;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.villamil.barberbooking.application.dto.command.ChangePasswordCommand;
import com.villamil.barberbooking.application.port.in.PasswordSecurityUseCase;
import com.villamil.barberbooking.application.port.out.PasswordHasherPort;
import com.villamil.barberbooking.application.port.out.PasswordSecurityRepositoryPort;
import com.villamil.barberbooking.application.port.out.UserAccountRepositoryPort;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;

@Service
public class PasswordSecurityService implements PasswordSecurityUseCase {
    private static final String INVALID_CHANGE = "Unable to change password with the supplied data";
    private final CurrentUserResolver users;
    private final UserAccountRepositoryPort accounts;
    private final PasswordHasherPort passwords;
    private final PasswordSecurityRepositoryPort security;

    PasswordSecurityService(CurrentUserResolver users, UserAccountRepositoryPort accounts,
            PasswordHasherPort passwords, PasswordSecurityRepositoryPort security) {
        this.users = users;
        this.accounts = accounts;
        this.passwords = passwords;
        this.security = security;
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        var actor = users.requireCurrentUser();
        if (command == null || !validNewPassword(command.newPassword())
                || command.oldPassword() == null || command.oldPassword().isBlank()
                || command.oldPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalidChange();
        }
        var account = accounts.findById(actor.id()).filter(user -> user.active()).orElse(null);
        if (account == null) {
            passwords.mitigateMissingAccount(command.oldPassword());
            throw invalidChange();
        }
        if (!passwords.matches(command.oldPassword(), account.passwordHash())
                || passwords.matches(command.newPassword(), account.passwordHash())) {
            throw invalidChange();
        }
        String replacement = passwords.hash(command.newPassword());
        if (!security.compareAndChangePassword(actor.id(), account.passwordHash(), replacement)) {
            throw invalidChange();
        }
    }

    private boolean validNewPassword(String password) {
        return password != null && !password.isBlank() && password.length() >= 12
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private BusinessRuleException invalidChange() {
        return new BusinessRuleException(INVALID_CHANGE);
    }
}
