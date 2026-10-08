package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.RegisterCompanyCommand;
import com.villamil.barberbooking.application.dto.response.CompanyContextResponse;

public interface CompanyOnboardingUseCase {
    CompanyContextResponse register(RegisterCompanyCommand command);
    CompanyContextResponse currentCompany();
}
