package com.villamil.barberbooking.domain.capabilities;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CompanyCapabilitiesTest {
    private static final Instant NOW = Instant.parse("2026-10-08T15:00:00Z");

    @Test void freeCompanyRetainsBasicBookingWithoutDelegatedManagement() {
        var rights = CompanyCapabilities.evaluate(new CompanyCapabilityStatus(true, true, null), NOW);
        assertEquals(CapabilityTier.FREE, rights.tier());
        assertTrue(rights.basicBooking());
        assertTrue(rights.companyActive());
        assertFalse(rights.teamManagement());
    }

    @Test void activePaidCompanyReceivesTeamManagement() {
        var rights = CompanyCapabilities.evaluate(new CompanyCapabilityStatus(true, true, NOW.plusSeconds(1)), NOW);
        assertEquals(CapabilityTier.BUSINESS, rights.tier());
        assertTrue(rights.basicBooking());
        assertTrue(rights.teamManagement());
    }

    @Test void expirationBoundaryRetainsBasicBookingButRevokesManagement() {
        var rights = CompanyCapabilities.evaluate(new CompanyCapabilityStatus(true, true, NOW), NOW);
        assertEquals(CapabilityTier.FREE, rights.tier());
        assertTrue(rights.basicBooking());
        assertFalse(rights.teamManagement());
    }

    @Test void suspendedCompanyCannotOperateEvenWithPaidValidity() {
        var rights = CompanyCapabilities.evaluate(new CompanyCapabilityStatus(false, true, NOW.plusSeconds(100)), NOW);
        assertEquals(CapabilityTier.BUSINESS, rights.tier());
        assertFalse(rights.basicBooking());
        assertFalse(rights.teamManagement());
        assertFalse(rights.companyActive());
    }

    @Test void inactiveBranchCannotOperateEvenWhenCompanyIsActive() {
        var rights = CompanyCapabilities.evaluate(new CompanyCapabilityStatus(true, false, NOW.plusSeconds(100)), NOW);
        assertFalse(rights.basicBooking());
        assertFalse(rights.teamManagement());
        assertFalse(rights.companyActive());
    }
}
