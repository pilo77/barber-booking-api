package com.villamil.barberbooking.infrastructure.security;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.FilterChain;
class AuthAbuseProtectionFilterTest {
    @Test void repeatedLoginIsLimitedAndForwardedHeadersCannotBypassIt() throws Exception {
        var filter = new AuthAbuseProtectionFilter();
        for (int i = 0; i < 21; i++) {
            var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.addHeader("X-Forwarded-For", "203.0.113." + i);
            var response = new MockHttpServletResponse(); var chain = mock(FilterChain.class);
            filter.doFilter(request, response, chain);
            if (i < 20) verify(chain).doFilter(request, response);
            else { assertEquals(429, response.getStatus()); verifyNoInteractions(chain); }
        }
    }
    @Test void healthChecksAreNeverCountedAsLoginAttempts() throws Exception {
        var filter = new AuthAbuseProtectionFilter(); var request = new MockHttpServletRequest("GET", "/actuator/health");
        var response = new MockHttpServletResponse(); var chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain); verify(chain).doFilter(request, response);
    }
    @Test void passwordChangesHaveIndependentTenPerAddressLimitWithoutReadingBody() throws Exception {
        var filter = new AuthAbuseProtectionFilter();
        for (int i = 0; i < 11; i++) {
            var request = new MockHttpServletRequest("POST", "/api/v1/auth/change-password");
            request.setRemoteAddr("203.0.113.5");
            request.addHeader("X-Forwarded-For", "203.0.113." + i);
            var response = new MockHttpServletResponse(); var chain = mock(FilterChain.class);
            filter.doFilter(request, response, chain);
            if (i < 10) verify(chain).doFilter(request, response);
            else { assertEquals(429, response.getStatus()); verifyNoInteractions(chain); }
        }
    }
    @Test void passwordChangesHaveGlobalFiftyPerMinuteBoundAcrossAddresses() throws Exception {
        var filter = new AuthAbuseProtectionFilter();
        for (int i = 0; i < 51; i++) {
            var request = new MockHttpServletRequest("POST", "/api/v1/auth/change-password");
            request.setRemoteAddr("203.0.113." + i);
            var response = new MockHttpServletResponse(); var chain = mock(FilterChain.class);
            filter.doFilter(request, response, chain);
            if (i < 50) verify(chain).doFilter(request, response);
            else { assertEquals(429, response.getStatus()); verifyNoInteractions(chain); }
        }
    }
}
