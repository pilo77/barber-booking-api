package com.villamil.barberbooking.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.villamil.barberbooking.application.port.out.BillingRepositoryPort;
import com.villamil.barberbooking.application.port.out.CapabilityRepositoryPort;
import com.villamil.barberbooking.domain.capabilities.CapabilityTier;
import com.villamil.barberbooking.domain.capabilities.CompanyCapabilities;
import com.villamil.barberbooking.domain.exception.PublicResourceNotFoundException;
import com.villamil.barberbooking.domain.model.CompanySubscription;

class PublicSubscriptionPolicyTest {
    private static final Instant NOW = Instant.parse("2026-10-08T15:00:00Z");
    private BillingRepositoryPort billing;
    private CapabilityRepositoryPort repository;
    private CapabilityService capabilities;

    @BeforeEach void setUp() {
        billing = mock(BillingRepositoryPort.class);
        repository = mock(CapabilityRepositoryPort.class);
        capabilities = mock(CapabilityService.class);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void publishedFreeBranchCanBeBookedRegardlessOfLegacyEnforcement(boolean enforced) {
        when(capabilities.forTenant(10L, 20L)).thenReturn(freeActive());
        when(repository.findPublicationState(10L, 20L)).thenReturn(Optional.of("PUBLISHED"));
        assertDoesNotThrow(() -> policy(enforced).requirePublicBooking(10L, 20L));
        verifyNoInteractions(billing);
    }

    @ParameterizedTest @ValueSource(strings = {"DRAFT", "PENDING_REVIEW", "HIDDEN", "SUSPENDED"})
    void explicitUnpublishedStateIsDeniedEvenWithLegacyEnforcementOff(String state) {
        when(capabilities.forTenant(10L, 20L)).thenReturn(new CompanyCapabilities(CapabilityTier.BUSINESS, true, true, true));
        when(repository.findPublicationState(10L, 20L)).thenReturn(Optional.of(state));
        assertThrows(PublicResourceNotFoundException.class,
                () -> policy(false).requirePublicBooking(10L, 20L));
        verifyNoInteractions(billing);
    }

    @Test void inactiveTenantCannotBeBookedEvenIfPublished() {
        when(capabilities.forTenant(10L, 20L)).thenReturn(CompanyCapabilities.unavailable());
        assertThrows(PublicResourceNotFoundException.class,
                () -> policy(false).requirePublicBooking(10L, 20L));
        verifyNoInteractions(repository, billing);
    }

    @Test void legacyLinkStillRequiresPaidValidityWhenEnforced() {
        when(capabilities.forTenant(10L, 20L)).thenReturn(freeActive());
        when(repository.findPublicationState(10L, 20L)).thenReturn(Optional.empty());
        when(billing.subscription(10L)).thenReturn(new CompanySubscription(10L, null));
        assertThrows(PublicResourceNotFoundException.class,
                () -> policy(true).requirePublicBooking(10L, 20L));
    }

    @Test void legacyPaidLinkRemainsAvailableWhenEnforced() {
        when(capabilities.forTenant(10L, 20L)).thenReturn(freeActive());
        when(repository.findPublicationState(10L, 20L)).thenReturn(Optional.empty());
        when(billing.subscription(10L)).thenReturn(new CompanySubscription(10L, NOW.plusSeconds(1)));
        assertDoesNotThrow(() -> policy(true).requirePublicBooking(10L, 20L));
    }

    @Test void legacyLinkKeepsPreviousBehaviorWhenEnforcementIsOff() {
        when(capabilities.forTenant(10L, 20L)).thenReturn(freeActive());
        when(repository.findPublicationState(10L, 20L)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> policy(false).requirePublicBooking(10L, 20L));
        verifyNoInteractions(billing);
    }

    @Test void creationLocksPublicationWhileAvailabilityUsesOrdinaryRead() {
        when(capabilities.forTenant(10L, 20L)).thenReturn(freeActive());
        when(repository.lockPublicationState(10L, 20L)).thenReturn(Optional.of("PUBLISHED"));
        assertDoesNotThrow(() -> policy(true).requirePublicBookingLocked(10L, 20L));
        verify(repository).lockPublicationState(10L, 20L);
        verify(repository, never()).findPublicationState(any(), any());
        verifyNoInteractions(billing);
    }

    @Test void hideWinningThePublicationLockRejectsNewCreation() {
        when(capabilities.forTenant(10L, 20L)).thenReturn(freeActive());
        when(repository.lockPublicationState(10L, 20L)).thenReturn(Optional.of("HIDDEN"));
        assertThrows(PublicResourceNotFoundException.class,
                () -> policy(false).requirePublicBookingLocked(10L, 20L));
        verifyNoInteractions(billing);
    }

    private PublicSubscriptionPolicy policy(boolean enforced) {
        return new PublicSubscriptionPolicy(billing, repository, capabilities,
                Clock.fixed(NOW, ZoneOffset.UTC), enforced);
    }

    private CompanyCapabilities freeActive() {
        return new CompanyCapabilities(CapabilityTier.FREE, true, false, true);
    }
}
