package com.villamil.barberbooking.domain.model;

import java.time.Instant;
import java.time.ZoneOffset;

public record CompanySubscription(Long companyId, Instant validUntil) {
    public boolean activeAt(Instant now) {
        return validUntil != null && validUntil.isAfter(now);
    }
    public Instant renewedUntil(Instant confirmedAt) {
        Instant start = activeAt(confirmedAt) ? validUntil : confirmedAt;
        return start.atOffset(ZoneOffset.UTC).plusMonths(1).toInstant();
    }
}
