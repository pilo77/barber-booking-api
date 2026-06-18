package com.villamil.barberbooking.infrastructure.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.infrastructure.security.AuthenticatedUserPrincipal;

import jakarta.servlet.ServletException;

class TemporaryTenantHeaderFilterTest {

	private final ThreadLocalTenantContextProvider tenantContextProvider = new ThreadLocalTenantContextProvider();
	private final TemporaryTenantHeaderFilter filter = new TemporaryTenantHeaderFilter(tenantContextProvider);

	@AfterEach
	void tearDown() {
		tenantContextProvider.clear();
		SecurityContextHolder.clearContext();
	}

	@Test
	void shouldUseDefaultTenantWhenHeadersAreMissing() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		CapturingFilterChain chain = new CapturingFilterChain(tenantContextProvider);

		filter.doFilter(request, response, chain);

		assertThat(chain.tenantContext).isEqualTo(TenantContext.DEFAULT);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void shouldUseTenantHeadersWhenPresent() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(TemporaryTenantHeaderFilter.COMPANY_HEADER, "2");
		request.addHeader(TemporaryTenantHeaderFilter.BRANCH_HEADER, "3");
		MockHttpServletResponse response = new MockHttpServletResponse();
		CapturingFilterChain chain = new CapturingFilterChain(tenantContextProvider);

		filter.doFilter(request, response, chain);

		assertThat(chain.tenantContext).isEqualTo(new TenantContext(2L, 3L));
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void shouldUseAuthenticatedUserTenantBeforeHeaders() throws ServletException, IOException {
		AuthenticatedUserResponse user = new AuthenticatedUserResponse(
				9L,
				"owner@example.com",
				"Owner User",
				7L,
				8L,
				null,
				Set.of(Role.COMPANY_OWNER)
		);
		AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
				user,
				Set.of(new SimpleGrantedAuthority("ROLE_COMPANY_OWNER"))
		);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
		);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(TemporaryTenantHeaderFilter.COMPANY_HEADER, "2");
		request.addHeader(TemporaryTenantHeaderFilter.BRANCH_HEADER, "3");
		MockHttpServletResponse response = new MockHttpServletResponse();
		CapturingFilterChain chain = new CapturingFilterChain(tenantContextProvider);

		filter.doFilter(request, response, chain);

		assertThat(chain.tenantContext).isEqualTo(new TenantContext(7L, 8L));
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void shouldNotUseTenantHeadersForAuthenticatedUserWithoutTenant() throws ServletException, IOException {
		AuthenticatedUserResponse user = new AuthenticatedUserResponse(
				9L,
				"platform@example.com",
				"Platform User",
				null,
				null,
				null,
				Set.of(Role.PLATFORM_OWNER)
		);
		AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
				user,
				Set.of(new SimpleGrantedAuthority("ROLE_PLATFORM_OWNER"))
		);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
		);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(TemporaryTenantHeaderFilter.COMPANY_HEADER, "2");
		request.addHeader(TemporaryTenantHeaderFilter.BRANCH_HEADER, "3");
		MockHttpServletResponse response = new MockHttpServletResponse();
		CapturingFilterChain chain = new CapturingFilterChain(tenantContextProvider);

		filter.doFilter(request, response, chain);

		assertThat(chain.tenantContext).isEqualTo(TenantContext.DEFAULT);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void shouldRejectInvalidTenantHeader() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(TemporaryTenantHeaderFilter.COMPANY_HEADER, "invalid");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(400);
	}

	private static class CapturingFilterChain extends MockFilterChain {

		private final ThreadLocalTenantContextProvider tenantContextProvider;
		private TenantContext tenantContext;

		private CapturingFilterChain(ThreadLocalTenantContextProvider tenantContextProvider) {
			this.tenantContextProvider = tenantContextProvider;
		}

		@Override
		public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
				throws IOException, ServletException {
			tenantContext = tenantContextProvider.currentTenant();
			super.doFilter(request, response);
		}
	}
}
