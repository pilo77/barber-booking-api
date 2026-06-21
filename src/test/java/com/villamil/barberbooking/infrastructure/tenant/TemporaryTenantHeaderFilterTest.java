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
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/customers");
		request.addHeader(TemporaryTenantHeaderFilter.COMPANY_HEADER, "2");
		request.addHeader(TemporaryTenantHeaderFilter.BRANCH_HEADER, "3");
		MockHttpServletResponse response = new MockHttpServletResponse();
		CapturingFilterChain chain = new CapturingFilterChain(tenantContextProvider);

		filter.doFilter(request, response, chain);

		assertThat(chain.tenantContext).isEqualTo(new TenantContext(2L, 3L));
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(tenantContextProvider.currentTenant()).isEqualTo(TenantContext.DEFAULT);
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
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/customers");
		request.addHeader(TemporaryTenantHeaderFilter.COMPANY_HEADER, "2");
		request.addHeader(TemporaryTenantHeaderFilter.BRANCH_HEADER, "3");
		MockHttpServletResponse response = new MockHttpServletResponse();
		CapturingFilterChain chain = new CapturingFilterChain(tenantContextProvider);

		filter.doFilter(request, response, chain);

		assertThat(chain.tenantContext).isEqualTo(new TenantContext(7L, 8L));
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(tenantContextProvider.currentTenant()).isEqualTo(TenantContext.DEFAULT);
	}

	@Test
	void shouldNotUseTenantHeadersForAuthenticatedUserWithoutTenantOnNonTenantRoute() throws ServletException, IOException {
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
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/user-accounts");
		request.addHeader(TemporaryTenantHeaderFilter.COMPANY_HEADER, "2");
		request.addHeader(TemporaryTenantHeaderFilter.BRANCH_HEADER, "3");
		MockHttpServletResponse response = new MockHttpServletResponse();
		CapturingFilterChain chain = new CapturingFilterChain(tenantContextProvider);

		filter.doFilter(request, response, chain);

		assertThat(chain.tenantContext).isEqualTo(TenantContext.DEFAULT);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void shouldRejectAuthenticatedUserWithoutTenantOnTenantScopedRoute() throws ServletException, IOException {
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
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/customers");
		request.addHeader(TemporaryTenantHeaderFilter.COMPANY_HEADER, "2");
		request.addHeader(TemporaryTenantHeaderFilter.BRANCH_HEADER, "3");
		MockHttpServletResponse response = new MockHttpServletResponse();
		CapturingFilterChain chain = new CapturingFilterChain(tenantContextProvider);

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(chain.tenantContext).isNull();
		assertThat(tenantContextProvider.currentTenant()).isEqualTo(TenantContext.DEFAULT);
	}

	@Test
	void shouldRejectInvalidTenantHeader() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/customers");
		request.addHeader(TemporaryTenantHeaderFilter.COMPANY_HEADER, "invalid");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(400);
	}

	@Test
	void shouldIgnoreTenantHeadersForPublicRoutes() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest(
				"GET",
				"/api/v1/public/barber-shops/ponte-perro"
		);
		request.addHeader(TemporaryTenantHeaderFilter.COMPANY_HEADER, "invalid");
		request.addHeader(TemporaryTenantHeaderFilter.BRANCH_HEADER, "999");
		MockHttpServletResponse response = new MockHttpServletResponse();
		CapturingFilterChain chain = new CapturingFilterChain(tenantContextProvider);

		filter.doFilter(request, response, chain);

		assertThat(chain.tenantContext).isEqualTo(TenantContext.DEFAULT);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void shouldIgnoreTenantHeadersForAuthRoutes() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
		request.addHeader(TemporaryTenantHeaderFilter.COMPANY_HEADER, "invalid");
		request.addHeader(TemporaryTenantHeaderFilter.BRANCH_HEADER, "999");
		MockHttpServletResponse response = new MockHttpServletResponse();
		CapturingFilterChain chain = new CapturingFilterChain(tenantContextProvider);

		filter.doFilter(request, response, chain);

		assertThat(chain.tenantContext).isEqualTo(TenantContext.DEFAULT);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void shouldIgnoreTenantHeadersForSwaggerRoutes() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
		request.addHeader(TemporaryTenantHeaderFilter.COMPANY_HEADER, "invalid");
		request.addHeader(TemporaryTenantHeaderFilter.BRANCH_HEADER, "999");
		MockHttpServletResponse response = new MockHttpServletResponse();
		CapturingFilterChain chain = new CapturingFilterChain(tenantContextProvider);

		filter.doFilter(request, response, chain);

		assertThat(chain.tenantContext).isEqualTo(TenantContext.DEFAULT);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void shouldRestorePreviousTenantAfterScopedPublicExecution() {
		tenantContextProvider.set(new TenantContext(2L, 3L));

		TenantContext inside = tenantContextProvider.withTenant(
				new TenantContext(7L, 9L),
				tenantContextProvider::currentTenant
		);

		assertThat(inside).isEqualTo(new TenantContext(7L, 9L));
		assertThat(tenantContextProvider.currentTenant()).isEqualTo(new TenantContext(2L, 3L));
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
