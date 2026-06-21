package com.villamil.barberbooking.infrastructure.tenant;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.infrastructure.security.AuthenticatedUserPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Component
public class TemporaryTenantHeaderFilter extends OncePerRequestFilter {

	public static final String COMPANY_HEADER = "X-Company-Id";
	public static final String BRANCH_HEADER = "X-Branch-Id";
	private static final String TENANT_SCOPE_REQUIRED = "Tenant-scoped access requires an authenticated company and branch";

	private final ThreadLocalTenantContextProvider tenantContextProvider;

	public TemporaryTenantHeaderFilter(ThreadLocalTenantContextProvider tenantContextProvider) {
		this.tenantContextProvider = tenantContextProvider;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		try {
			TenantContext tenantContext = resolveTenant(request);
			tenantContextProvider.set(tenantContext);
			filterChain.doFilter(request, response);
		}
		catch (UnscopedAuthenticatedTenantAccessException exception) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, exception.getMessage());
		}
		catch (IllegalArgumentException exception) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
		}
		finally {
			tenantContextProvider.clear();
		}
	}

	private TenantContext resolveTenant(HttpServletRequest request) {
		String path = request.getRequestURI();
		if (!isTenantScopedRoute(path)) {
			return TenantContext.DEFAULT;
		}
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null
				&& authentication.isAuthenticated()
				&& authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
			if (principal.user().companyId() != null && principal.user().branchId() != null) {
				return new TenantContext(principal.user().companyId(), principal.user().branchId());
			}
			throw new UnscopedAuthenticatedTenantAccessException(TENANT_SCOPE_REQUIRED);
		}
		Long companyId = parsePositiveHeader(request, COMPANY_HEADER, TenantContext.DEFAULT_COMPANY_ID);
		Long branchId = parsePositiveHeader(request, BRANCH_HEADER, TenantContext.DEFAULT_BRANCH_ID);
		return new TenantContext(companyId, branchId);
	}

	private boolean isTenantScopedRoute(String path) {
		return path.startsWith("/api/v1/customers")
				|| path.startsWith("/api/v1/barbers")
				|| path.startsWith("/api/v1/services")
				|| path.startsWith("/api/v1/appointments")
				|| path.startsWith("/api/v1/companies/public-profile");
	}

	private Long parsePositiveHeader(HttpServletRequest request, String headerName, Long defaultValue) {
		String rawValue = request.getHeader(headerName);
		if (rawValue == null || rawValue.isBlank()) {
			return defaultValue;
		}
		try {
			long value = Long.parseLong(rawValue);
			if (value <= 0) {
				throw new IllegalArgumentException(headerName + " must be positive");
			}
			return value;
		}
		catch (NumberFormatException exception) {
			throw new IllegalArgumentException(headerName + " must be a valid number");
		}
	}

	private static final class UnscopedAuthenticatedTenantAccessException extends RuntimeException {
		private UnscopedAuthenticatedTenantAccessException(String message) {
			super(message);
		}
	}
}
