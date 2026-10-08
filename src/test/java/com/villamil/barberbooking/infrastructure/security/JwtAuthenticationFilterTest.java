package com.villamil.barberbooking.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.port.out.JwtTokenPort;
import com.villamil.barberbooking.application.port.out.UserAccountRepositoryPort;
import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.domain.model.UserAccount;

class JwtAuthenticationFilterTest {
    private final JwtTokenPort tokens = mock(JwtTokenPort.class);
    private final UserAccountRepositoryPort accounts = mock(UserAccountRepositoryPort.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokens, accounts);

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void activeAccountCanAuthenticate() throws Exception {
        UserAccount account = account(true, Set.of(Role.RECEPTIONIST));
        authenticate(account, account);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test void deactivatedAccountCannotReusePreviouslyIssuedToken() throws Exception {
        authenticate(account(true, Set.of(Role.RECEPTIONIST)), account(false, Set.of(Role.RECEPTIONIST)));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test void changedRolesInvalidatePreviouslyIssuedToken() throws Exception {
        authenticate(account(true, Set.of(Role.COMPANY_OWNER)), account(true, Set.of(Role.RECEPTIONIST)));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test void missingAccountCannotAuthenticate() throws Exception {
        when(tokens.parse("test-token")).thenReturn(AuthenticatedUserResponse.from(account(true, Set.of(Role.RECEPTIONIST))));
        perform();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void authenticate(UserAccount original, UserAccount current) throws Exception {
        when(tokens.parse("test-token")).thenReturn(AuthenticatedUserResponse.from(original));
        when(accounts.findById(9L)).thenReturn(Optional.of(current));
        perform();
    }
    private void perform() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/customers");
        request.addHeader("Authorization", "Bearer test-token");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }
    private UserAccount account(boolean active, Set<Role> roles) {
        return new UserAccount(9L, 1L, 1L, "user@example.com", "test-hash", "User", null,
                null, active, java.time.Instant.EPOCH, java.time.Instant.EPOCH, roles);
    }
}
