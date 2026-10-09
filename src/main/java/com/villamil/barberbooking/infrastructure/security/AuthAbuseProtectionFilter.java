package com.villamil.barberbooking.infrastructure.security;

import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Bounded protection for the single-instance free deployment. Forwarded IP headers are not trusted. */
@Component
public class AuthAbuseProtectionFilter extends OncePerRequestFilter {
    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<>();
    public AuthAbuseProtectionFilter() { this(Clock.systemUTC()); }
    AuthAbuseProtectionFilter(Clock clock) { this.clock = clock; }
    private record Window(long startedAt, int count) {}
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean registration = "/api/v1/auth/register-company".equals(path);
        boolean passwordChange = "/api/v1/auth/change-password".equals(path);
        if (!"POST".equals(request.getMethod()) || (!registration && !passwordChange && !"/api/v1/auth/login".equals(path))) {
            chain.doFilter(request, response); return;
        }
        long seconds = registration ? 3600 : 60;
        int perAddress = registration ? 5 : passwordChange ? 10 : 20;
        int global = registration ? 20 : passwordChange ? 50 : 100;
        if (!allow(path + ":all", global, seconds)
                || !allow(path + ":" + request.getRemoteAddr(), perAddress, seconds)) {
            response.setStatus(429); response.setContentType("application/problem+json");
            response.setHeader("Retry-After", Long.toString(seconds));
            response.getWriter().write("{\"status\":429,\"title\":\"Too many requests\",\"detail\":\"Please wait before trying again\"}");
            return;
        }
        chain.doFilter(request, response);
    }
    private synchronized boolean allow(String key, int limit, long seconds) {
        long now = clock.instant().getEpochSecond();
        windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt() >= 3600);
        Window window = windows.get(key);
        if (window == null || now - window.startedAt() >= seconds) {
            if (windows.size() >= 10000 && window == null) return false;
            windows.put(key, new Window(now, 1)); return true;
        }
        if (window.count() >= limit) return false;
        windows.put(key, new Window(window.startedAt(), window.count() + 1)); return true;
    }
}
