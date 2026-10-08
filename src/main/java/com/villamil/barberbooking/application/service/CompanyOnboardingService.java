package com.villamil.barberbooking.application.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.villamil.barberbooking.application.dto.command.RegisterCompanyCommand;
import com.villamil.barberbooking.application.dto.response.CompanyContextResponse;
import com.villamil.barberbooking.application.port.in.CompanyOnboardingUseCase;
import com.villamil.barberbooking.application.port.out.CompanyRegistrationPort;
import com.villamil.barberbooking.application.port.out.PasswordHasherPort;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;

@Service
class CompanyOnboardingService implements CompanyOnboardingUseCase {
    private final CompanyRegistrationPort companies;
    private final PasswordHasherPort passwords;
    private final CurrentUserResolver users;
    CompanyOnboardingService(CompanyRegistrationPort companies, PasswordHasherPort passwords, CurrentUserResolver users) {
        this.companies = companies; this.passwords = passwords; this.users = users;
    }
    @Override @Transactional
    public CompanyContextResponse register(RegisterCompanyCommand command) {
        String slug = text(command.companySlug()).toLowerCase(Locale.ROOT);
        if (!slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*") || slug.length() < 3 || slug.length() > 100)
            throw new BusinessRuleException("Company slug must be 3 to 100 lowercase letters, digits or hyphens");
        String email = text(command.email()).toLowerCase(Locale.ROOT);
        if (email.length() > 120 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new BusinessRuleException("A valid email is required");
        if (command.password() == null || command.password().length() < 12
                || command.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new BusinessRuleException("Password must be at least 12 characters and at most 72 UTF-8 bytes");
        return companies.create(text(command.companyName()), slug, text(command.branchName()),
                text(command.fullName()), email, passwords.hash(command.password()));
    }
    @Override @Transactional(readOnly = true)
    public CompanyContextResponse currentCompany() {
        var user = users.requireCurrentUser();
        if (user.companyId() == null || user.branchId() == null)
            throw new ForbiddenOperationException("An assigned company and branch are required");
        return companies.context(user.companyId(), user.branchId());
    }
    private String text(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 120)
            throw new BusinessRuleException("Required company registration data is invalid");
        return value.strip();
    }
}
