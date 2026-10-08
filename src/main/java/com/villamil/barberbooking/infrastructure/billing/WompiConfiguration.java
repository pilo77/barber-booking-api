package com.villamil.barberbooking.infrastructure.billing;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Secrets intentionally have no generated toString or getters exposed to web DTOs. */
@Component
public class WompiConfiguration {
    final boolean enabled;
    final String environment;
    final String publicKey;
    final String privateKey;
    final String integritySecret;
    final String eventsSecret;
    final String redirectUrl;

    public WompiConfiguration(@Value("${billing.wompi.enabled:false}") boolean enabled,
            @Value("${billing.wompi.environment:test}") String environment,
            @Value("${billing.wompi.public-key:}") String publicKey,
            @Value("${billing.wompi.private-key:}") String privateKey,
            @Value("${billing.wompi.integrity-secret:}") String integritySecret,
            @Value("${billing.wompi.events-secret:}") String eventsSecret,
            @Value("${billing.wompi.redirect-url:}") String redirectUrl) {
        if (!"test".equals(environment) && !"prod".equals(environment))
            throw new IllegalStateException("Invalid payment environment");
        if (enabled) {
            if (!publicKey.startsWith("pub_" + environment + "_")
                    || !privateKey.startsWith("prv_" + environment + "_")
                    || !integritySecret.startsWith(environment + "_integrity_")
                    || !eventsSecret.startsWith(environment + "_events_"))
                throw new IllegalStateException("Payment credentials must match the configured environment");
            URI redirect = URI.create(redirectUrl);
            if (!"https".equals(redirect.getScheme()) || redirect.getHost() == null || redirect.getUserInfo() != null
                    || redirect.getFragment() != null || redirect.getQuery() != null)
                throw new IllegalStateException("Payment redirect must be an HTTPS URL without credentials, query or fragment");
        }
        this.enabled = enabled;
        this.environment = environment;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.integritySecret = integritySecret;
        this.eventsSecret = eventsSecret;
        this.redirectUrl = redirectUrl;
    }
}
