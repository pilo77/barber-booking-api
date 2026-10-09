package com.villamil.barberbooking.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.service.CapabilityService;
import com.villamil.barberbooking.domain.capabilities.CapabilityTier;
import com.villamil.barberbooking.domain.capabilities.CompanyCapabilities;
import com.villamil.barberbooking.domain.model.Role;
import jakarta.servlet.FilterChain;

class SubscriptionAccessFilterTest {
    private CapabilityService capabilities;
    private SubscriptionAccessFilter filter;
    private FilterChain chain;

    @Test void suspendedTenantAndTeamChecksRemainRegisteredWhenLegacyBillingFlagIsFalse() {
        new ApplicationContextRunner().withUserConfiguration(SubscriptionAccessFilter.class)
                .withBean(CapabilityService.class, () -> mock(CapabilityService.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withPropertyValues("billing.enforce-subscription=false")
                .run(context -> assertNotNull(context.getBean(SubscriptionAccessFilter.class)));
    }

    @Test void suspendedCompanyCannotEditItsMarketplaceProfile() throws Exception {
        authenticate(Role.COMPANY_OWNER);
        when(capabilities.forTenant(10L, 20L)).thenReturn(CompanyCapabilities.unavailable());
        var response = apply("/api/v1/company/marketplace-profile");
        assertEquals(403, response.getStatus());
        verifyNoInteractions(chain);
    }

    @BeforeEach void setUp() {
        capabilities = mock(CapabilityService.class);
        filter = new SubscriptionAccessFilter(capabilities, new ObjectMapper());
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach void clearSession() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest @ValueSource(strings = {"/api/v1/customers", "/api/v1/barbers", "/api/v1/barbers/2/working-hours",
            "/api/v1/services", "/api/v1/appointments/1/complete", "/api/v1/companies/public-profile"})
    void freeOwnerRetainsEssentialOperations(String path) throws Exception {
        authenticate(Role.COMPANY_OWNER);
        when(capabilities.forTenant(10L, 20L)).thenReturn(freeActive());
        var request = new MockHttpServletRequest("GET", path);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test void freeOwnerCannotManageTeamAccounts() throws Exception {
        authenticate(Role.COMPANY_OWNER);
        when(capabilities.forTenant(10L, 20L)).thenReturn(freeActive());
        var response = apply("/api/v1/user-accounts");
        assertEquals(402, response.getStatus());
        assertTrue(response.getContentAsString().contains("SUBSCRIPTION_REQUIRED"));
        verifyNoInteractions(chain);
    }

    @Test void paidOwnerCanManageTeamAccounts() throws Exception {
        authenticate(Role.COMPANY_OWNER);
        when(capabilities.forTenant(10L, 20L)).thenReturn(new CompanyCapabilities(CapabilityTier.BUSINESS, true, true, true));
        apply("/api/v1/user-accounts");
        verify(chain).doFilter(any(), any());
    }

    @Test void platformOwnerCanManageAccountsWithoutAssignedTenantOrSubscription() throws Exception {
        authenticate(Role.PLATFORM_OWNER);
        apply("/api/v1/user-accounts");
        verify(chain).doFilter(any(), any());
        verifyNoInteractions(capabilities);
    }

    @Test void freeBarberRetainsAttentionOfOwnAppointmentsBeforeOwnershipCheck() throws Exception {
        authenticate(Role.BARBER);
        when(capabilities.forTenant(10L, 20L)).thenReturn(freeActive());
        apply("/api/v1/appointments/1/start");
        verify(chain).doFilter(any(), any());
    }

    @Test void invalidRoleReachesAuthorizationInsteadOfReceivingCommercialError() throws Exception {
        authenticate(Role.BARBER);
        when(capabilities.forTenant(10L, 20L)).thenReturn(freeActive());
        apply("/api/v1/user-accounts");
        verify(chain).doFilter(any(), any());
    }

    @Test void suspendedCompanyIsDeniedEvenWhenPaid() throws Exception {
        authenticate(Role.COMPANY_OWNER);
        when(capabilities.forTenant(10L, 20L)).thenReturn(new CompanyCapabilities(CapabilityTier.BUSINESS, false, false, false));
        var response = apply("/api/v1/appointments");
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("COMPANY_SUSPENDED"));
        verifyNoInteractions(chain);
    }

    @Test void unauthenticatedRequestReachesExistingAuthenticationEntryPoint() throws Exception {
        apply("/api/v1/customers");
        verify(chain).doFilter(any(), any());
        verifyNoInteractions(capabilities);
    }

    @Test void billingAndCapabilitiesRemainAccessibleForSuspendedOwner() throws Exception {
        authenticate(Role.COMPANY_OWNER);
        apply("/api/v1/billing/subscription");
        apply("/api/v1/company/capabilities");
        verify(chain, times(2)).doFilter(any(), any());
        verifyNoInteractions(capabilities);
    }

    private MockHttpServletResponse apply(String path) throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", path), response, chain);
        return response;
    }

    private void authenticate(Role role) {
        var user = new AuthenticatedUserResponse(1L, "qa@example.test", "QA User",
                role == Role.PLATFORM_OWNER ? null : 10L,
                role == Role.PLATFORM_OWNER ? null : 20L,
                role == Role.BARBER ? 2L : null, Set.of(role));
        var principal = new AuthenticatedUserPrincipal(user, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private CompanyCapabilities freeActive() {
        return new CompanyCapabilities(CapabilityTier.FREE, true, false, true);
    }
}
