package com.villamil.barberbooking.domain.capabilities;

import java.time.Instant;
import java.util.Objects;

/** Commercial rights never replace authorization or tenant isolation. */
public record CompanyCapabilities(
        CapabilityTier tier,
        boolean basicBooking,
        boolean teamManagement,
        boolean companyActive
) {
    public static CompanyCapabilities evaluate(CompanyCapabilityStatus status, Instant now) {
        Objects.requireNonNull(status, "Capability status is required");
        Objects.requireNonNull(now, "Evaluation time is required");
        boolean paid = status.subscriptionValidUntil() != null
                && status.subscriptionValidUntil().isAfter(now);
        boolean active = status.tenantActive();
        return new CompanyCapabilities(paid ? CapabilityTier.BUSINESS : CapabilityTier.FREE,
                active, active && paid, active);
    }

    public static CompanyCapabilities unavailable() {
        return new CompanyCapabilities(CapabilityTier.FREE, false, false, false);
    }
}
