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
		catch (IllegalArgumentException exception) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
		}
		finally {
			tenantContextProvider.clear();
		}
	}

	private TenantContext resolveTenant(HttpServletRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null
				&& authentication.isAuthenticated()
				&& authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal
				&& principal.user().companyId() != null
				&& principal.user().branchId() != null) {
			return new TenantContext(principal.user().companyId(), principal.user().branchId());
		}
		Long companyId = parsePositiveHeader(request, COMPANY_HEADER, TenantContext.DEFAULT_COMPANY_ID);
		Long branchId = parsePositiveHeader(request, BRANCH_HEADER, TenantContext.DEFAULT_BRANCH_ID);
		return new TenantContext(companyId, branchId);
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
}
