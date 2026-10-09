package com.villamil.barberbooking.domain.capabilities;

import java.time.Instant;

public record CompanyCapabilityStatus(
        boolean companyActive,
        boolean branchActive,
        Instant subscriptionValidUntil
) {
    public boolean tenantActive() {
        return companyActive && branchActive;
    }
}
