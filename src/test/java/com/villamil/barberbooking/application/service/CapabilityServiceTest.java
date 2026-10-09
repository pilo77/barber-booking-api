package com.villamil.barberbooking.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.port.out.CapabilityRepositoryPort;
import com.villamil.barberbooking.domain.capabilities.CapabilityTier;
import com.villamil.barberbooking.domain.capabilities.CompanyCapabilityStatus;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;
import com.villamil.barberbooking.domain.model.Role;

class CapabilityServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-08T15:00:00Z");
    private CapabilityRepositoryPort repository;
    private CurrentUserResolver users;
    private CapabilityService service;

    @BeforeEach void setUp() {
        repository = mock(CapabilityRepositoryPort.class);
        users = mock(CurrentUserResolver.class);
        service = new CapabilityService(repository, users, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test void currentCapabilitiesUseAuthenticatedTenantOnly() {
        when(users.requireCurrentUser()).thenReturn(user(Role.COMPANY_OWNER, 10L, 20L));
        when(repository.findStatus(10L, 20L)).thenReturn(Optional.of(new CompanyCapabilityStatus(true, true, null)));
        var response = service.currentCapabilities();
        assertEquals(CapabilityTier.FREE, response.tier());
        assertTrue(response.basicBooking());
        assertFalse(response.teamManagement());
        verify(repository).findStatus(10L, 20L);
    }

    @Test void businessIsEvaluatedWithInjectedClock() {
        when(repository.findStatus(10L, 20L)).thenReturn(Optional.of(new CompanyCapabilityStatus(true, true, NOW.plusSeconds(1))));
        assertTrue(service.forTenant(10L, 20L).teamManagement());
    }

    @Test void missingOrCrossTenantBranchHasNoRights() {
        when(repository.findStatus(10L, 99L)).thenReturn(Optional.empty());
        var rights = service.forTenant(10L, 99L);
        assertFalse(rights.companyActive());
        assertFalse(rights.basicBooking());
        assertFalse(rights.teamManagement());
    }

    @Test void missingTenantFailsClosedWithoutQuery() {
        assertFalse(service.forTenant(null, null).companyActive());
        verifyNoInteractions(repository);
    }

    @Test void customerCannotReadOperationalCapabilities() {
        when(users.requireCurrentUser()).thenReturn(user(Role.CUSTOMER, 10L, 20L));
        assertThrows(ForbiddenOperationException.class, service::currentCapabilities);
        verifyNoInteractions(repository);
    }

    @Test void platformAccountWithoutTenantCannotReadCompanyCapabilities() {
        when(users.requireCurrentUser()).thenReturn(user(Role.PLATFORM_OWNER, null, null));
        assertThrows(ForbiddenOperationException.class, service::currentCapabilities);
        verifyNoInteractions(repository);
    }

    private AuthenticatedUserResponse user(Role role, Long company, Long branch) {
        return new AuthenticatedUserResponse(1L, "owner@example.test", "QA User", company, branch,
                role == Role.BARBER ? 2L : null, Set.of(role));
    }
}
