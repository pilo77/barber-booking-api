package com.villamil.barberbooking.application.dto.response;

import com.villamil.barberbooking.domain.capabilities.CapabilityTier;
import com.villamil.barberbooking.domain.capabilities.CompanyCapabilities;

public record CompanyCapabilitiesResponse(
        CapabilityTier tier,
        boolean basicBooking,
        boolean teamManagement,
        boolean companyActive
) {
    public static CompanyCapabilitiesResponse from(CompanyCapabilities capabilities) {
        return new CompanyCapabilitiesResponse(capabilities.tier(), capabilities.basicBooking(),
                capabilities.teamManagement(), capabilities.companyActive());
    }
}
