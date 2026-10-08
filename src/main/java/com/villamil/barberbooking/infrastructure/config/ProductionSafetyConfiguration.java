package com.villamil.barberbooking.infrastructure.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class ProductionSafetyConfiguration {
    private static final String TRUSTED_SSL_FACTORY = "org.postgresql.ssl.DefaultJavaSSLFactory";

    public ProductionSafetyConfiguration(@Value("${spring.datasource.url}") String databaseUrl,
            @Value("${spring.datasource.username}") String username, @Value("${spring.datasource.password}") String password,
            @Value("${billing.enforce-subscription:false}") boolean enforceSubscription,
            @Value("${billing.wompi.environment}") String paymentEnvironment,
            @Value("${spring.datasource.hikari.data-source-properties.sslfactory:}") String sslFactory,
            @Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        if (!databaseUrl.startsWith("jdbc:postgresql://")) throw new IllegalStateException("Production requires PostgreSQL");
        URI database;
        try { database = URI.create(databaseUrl.substring(5)); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("Invalid production database configuration"); }
        String host = database.getHost();
        Map<String, List<String>> parameters = queryParameters(database.getRawQuery());
        List<String> factoryOverrides = parameters.getOrDefault("sslfactory", List.of());
        if (host == null || isLocalHost(host)
                || database.getUserInfo() != null || database.getFragment() != null
                || !parameters.getOrDefault("sslmode", List.of()).equals(List.of("verify-full"))
                || !TRUSTED_SSL_FACTORY.equals(sslFactory)
                || !(factoryOverrides.isEmpty() || factoryOverrides.equals(List.of(TRUSTED_SSL_FACTORY)))
                || parameters.containsKey("sslhostnameverifier")
                || username.isBlank() || password.isBlank())
            throw new IllegalStateException("Production requires a remote database, separate credentials and verified TLS");
        if (!enforceSubscription || !"prod".equals(paymentEnvironment))
            throw new IllegalStateException("Production requires subscription enforcement and production payment environment");
        validateOrigins(allowedOrigins);
    }

    private static Map<String, List<String>> queryParameters(String query) {
        Map<String, List<String>> parameters = new HashMap<>();
        if (query == null) return parameters;
        try {
            for (String part : query.split("&", -1)) {
                String[] pair = part.split("=", 2);
                String name = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String value = pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
                parameters.computeIfAbsent(name, unused -> new ArrayList<>()).add(value);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid production database configuration");
        }
        return parameters;
    }

    private static void validateOrigins(String allowedOrigins) {
        if (allowedOrigins.isBlank())
            throw new IllegalStateException("Production requires explicit HTTPS frontend origins");
        for (String value : allowedOrigins.split(",", -1)) {
            URI origin;
            try { origin = URI.create(value.trim()); }
            catch (IllegalArgumentException e) {
                throw new IllegalStateException("Invalid production frontend origin configuration");
            }
            if (!"https".equals(origin.getScheme()) || origin.getHost() == null || isLocalHost(origin.getHost())
                    || origin.getUserInfo() != null || origin.getQuery() != null || origin.getFragment() != null
                    || !origin.getRawPath().isEmpty())
                throw new IllegalStateException("Production requires explicit HTTPS frontend origins");
        }
    }

    private static boolean isLocalHost(String host) {
        String normalized = host.toLowerCase(java.util.Locale.ROOT).replaceAll("\\.$", "");
        return normalized.equals("localhost") || normalized.endsWith(".localhost")
                || normalized.startsWith("127.") || normalized.equals("0.0.0.0")
                || normalized.equals("::1") || normalized.equals("[::1]")
                || normalized.equals("::") || normalized.equals("[::]");
    }
}
