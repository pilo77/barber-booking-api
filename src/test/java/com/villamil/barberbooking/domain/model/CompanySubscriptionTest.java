package com.villamil.barberbooking.domain.model;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
class CompanySubscriptionTest {
    @Test void activeRenewalExtendsFromExpiryAndHandlesMonthEnd() {
        Instant now = Instant.parse("2026-01-10T12:00:00Z");
        assertEquals(Instant.parse("2026-02-28T12:00:00Z"), new CompanySubscription(1L, Instant.parse("2026-01-31T12:00:00Z")).renewedUntil(now));
    }
    @Test void expiredRenewalStartsFromConfirmation() {
        Instant now = Instant.parse("2026-03-10T12:00:00Z");
        assertEquals(Instant.parse("2026-04-10T12:00:00Z"), new CompanySubscription(1L, Instant.parse("2026-01-01T00:00:00Z")).renewedUntil(now));
    }
    @Test void pendingAndExactlyExpiredAreInactive() {
        Instant now = Instant.now(); assertFalse(new CompanySubscription(1L, null).activeAt(now));
        assertFalse(new CompanySubscription(1L, now).activeAt(now));
    }
}
