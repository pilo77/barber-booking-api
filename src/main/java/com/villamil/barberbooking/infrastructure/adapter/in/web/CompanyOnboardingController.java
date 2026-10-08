package com.villamil.barberbooking.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.villamil.barberbooking.application.dto.command.RegisterCompanyCommand;
import com.villamil.barberbooking.application.dto.response.CompanyContextResponse;
import com.villamil.barberbooking.application.port.in.CompanyOnboardingUseCase;

@RestController
public class CompanyOnboardingController {
    private final CompanyOnboardingUseCase companies;
    public CompanyOnboardingController(CompanyOnboardingUseCase companies) { this.companies = companies; }
    @PostMapping("/api/v1/auth/register-company") @ResponseStatus(HttpStatus.CREATED)
    public CompanyContextResponse register(@Valid @RequestBody RegistrationRequest request) {
        return companies.register(new RegisterCompanyCommand(request.companyName(), request.companySlug(),
                request.branchName(), request.fullName(), request.email(), request.password()));
    }
    @GetMapping("/api/v1/company/context") public CompanyContextResponse currentCompany() {
        return companies.currentCompany();
    }
    public record RegistrationRequest(@NotBlank @Size(max=120) String companyName,
            @NotBlank @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(min=3,max=100) String companySlug,
            @NotBlank @Size(max=120) String branchName, @NotBlank @Size(max=120) String fullName,
            @NotBlank @Email @Size(max=120) String email, @NotBlank @Size(min=12,max=72) String password) {
        @Override public String toString() { return "RegistrationRequest[redacted]"; }
    }
}
