package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.UpdateCompanyPublicBrandingCommand;
import com.villamil.barberbooking.application.dto.response.CompanyPublicBrandingResponse;

public interface UpdateCompanyPublicBrandingUseCase {

	CompanyPublicBrandingResponse update(UpdateCompanyPublicBrandingCommand command);
}
