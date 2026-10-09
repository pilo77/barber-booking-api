package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.response.CompanyCapabilitiesResponse;

public interface CompanyCapabilitiesUseCase {
    CompanyCapabilitiesResponse currentCapabilities();
}
