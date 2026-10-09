package com.villamil.barberbooking.infrastructure.security;

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villamil.barberbooking.application.service.CapabilityService;
import com.villamil.barberbooking.domain.model.Role;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

@Component
public class SubscriptionAccessFilter extends OncePerRequestFilter {
    private final CapabilityService capabilities;
    private final ObjectMapper mapper;
    public SubscriptionAccessFilter(CapabilityService capabilities, ObjectMapper mapper) {
        this.capabilities = capabilities;
        this.mapper = mapper;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean operational = resource(path, "/api/v1/customers") || resource(path, "/api/v1/barbers")
                || resource(path, "/api/v1/services") || resource(path, "/api/v1/appointments")
                || resource(path, "/api/v1/user-accounts") || resource(path, "/api/v1/companies/public-profile")
                || resource(path, "/api/v1/company/marketplace-profile");
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (operational && auth != null && auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal
                && !principal.user().roles().contains(Role.PLATFORM_OWNER)) {
            var rights = capabilities.forTenant(principal.user().companyId(), principal.user().branchId());
            if (!rights.companyActive()) {
                writeProblem(response, org.springframework.http.HttpStatus.FORBIDDEN,
                        "Company operations are unavailable", "COMPANY_SUSPENDED");
                return;
            }
            boolean canManageAccounts = principal.user().roles().contains(Role.COMPANY_OWNER)
                    || principal.user().roles().contains(Role.BRANCH_MANAGER);
            if (resource(path, "/api/v1/user-accounts") && canManageAccounts && !rights.teamManagement()) {
                writeProblem(response, org.springframework.http.HttpStatus.PAYMENT_REQUIRED,
                        "Business management is required to manage team accounts", "SUBSCRIPTION_REQUIRED");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean resource(String path, String base) {
        return path.equals(base) || path.startsWith(base + "/");
    }

    private void writeProblem(HttpServletResponse response, org.springframework.http.HttpStatus status,
            String detail, String code) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        mapper.writeValue(response.getOutputStream(), problem);
    }
}
