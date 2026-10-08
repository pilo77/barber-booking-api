package com.villamil.barberbooking.infrastructure.security;

import java.io.IOException;
import java.time.Instant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villamil.barberbooking.application.port.out.BillingRepositoryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

@Component
@ConditionalOnProperty(name="billing.enforce-subscription", havingValue="true")
public class SubscriptionAccessFilter extends OncePerRequestFilter {
    private final BillingRepositoryPort billing;
    private final ObjectMapper mapper;
    public SubscriptionAccessFilter(BillingRepositoryPort billing, ObjectMapper mapper) {
        this.billing = billing; this.mapper = mapper;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean operational = path.startsWith("/api/v1/customers") || path.startsWith("/api/v1/barbers")
                || path.startsWith("/api/v1/services") || path.startsWith("/api/v1/appointments")
                || path.startsWith("/api/v1/user-accounts") || path.startsWith("/api/v1/companies/public-profile");
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (operational && auth != null && auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal
                && principal.user().companyId() != null
                && !billing.subscription(principal.user().companyId()).activeAt(Instant.now())) {
            response.setStatus(402);
            response.setContentType("application/problem+json");
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(org.springframework.http.HttpStatus.PAYMENT_REQUIRED, "An active subscription is required");
            problem.setProperty("code", "SUBSCRIPTION_REQUIRED");
            mapper.writeValue(response.getOutputStream(), problem);
            return;
        }
        chain.doFilter(request, response);
    }
}
