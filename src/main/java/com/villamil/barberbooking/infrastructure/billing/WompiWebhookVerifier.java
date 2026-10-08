package com.villamil.barberbooking.infrastructure.billing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.villamil.barberbooking.application.billing.VerifiedPayment;
import com.villamil.barberbooking.application.exception.BillingUnavailableException;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;

@Component
public class WompiWebhookVerifier {
    private final WompiConfiguration config;
    private final WompiTransactionLookup transactions;
    public WompiWebhookVerifier(WompiConfiguration config, WompiTransactionLookup transactions) {
        this.config = config; this.transactions = transactions;
    }

    public Optional<VerifiedPayment> verify(JsonNode event) {
        if (!config.enabled) throw new BillingUnavailableException();
        if (!event.isObject() || !config.environment.equals(event.path("environment").asText())) throw invalid();
        JsonNode properties = event.path("signature").path("properties");
        JsonNode timestamp = event.path("timestamp");
        if (!properties.isArray() || properties.size() == 0 || properties.size() > 20 || !timestamp.isIntegralNumber()) throw invalid();
        long age = Instant.now().getEpochSecond() - timestamp.longValue();
        // Allow the provider's documented retry window without accepting indefinite replays.
        if (age < -300 || age > 172800) throw invalid();
        var values = new StringBuilder();
        Set<String> signed = new HashSet<>();
        for (JsonNode property : properties) {
            String path = property.asText();
            if (!path.matches("[A-Za-z0-9_.]{1,120}") || !signed.add(path)) throw invalid();
            JsonNode value = event.path("data");
            for (String part : path.split("\\.")) value = value.path(part);
            if (value.isMissingNode() || value.isNull() || value.isContainerNode()) throw invalid();
            values.append(value.asText());
        }
        String received = event.path("signature").path("checksum").asText();
        if (!received.matches("[A-Fa-f0-9]{64}")) throw invalid();
        String expected = WompiCheckoutAdapter.sha256(values.append(timestamp.longValue()).append(config.eventsSecret).toString());
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                received.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII))) throw invalid();
        if (!"transaction.updated".equals(event.path("event").asText())) return Optional.empty();
        if (!signed.containsAll(Set.of("transaction.id", "transaction.status", "transaction.amount_in_cents"))) throw invalid();
        JsonNode notified = event.path("data").path("transaction");
        if (!notified.path("amount_in_cents").isIntegralNumber()) throw invalid();
        JsonNode confirmed = transactions.find(notified.path("id").asText());
        for (String field : Set.of("id", "status", "amount_in_cents", "currency", "reference")) {
            if (!confirmed.hasNonNull(field) || !confirmed.path(field).equals(notified.path(field))) throw invalid();
        }
        if (!confirmed.path("amount_in_cents").isIntegralNumber()) throw invalid();
        String reference = confirmed.path("reference").asText();
        // A merchant can also receive payments outside this SaaS. Acknowledge unrelated references.
        try { UUID.fromString(reference); } catch (IllegalArgumentException e) { return Optional.empty(); }
        return Optional.of(new VerifiedPayment(reference, confirmed.path("id").asText(),
                confirmed.path("amount_in_cents").longValue(), confirmed.path("currency").asText(),
                config.environment, confirmed.path("status").asText()));
    }
    private ForbiddenOperationException invalid() { return new ForbiddenOperationException("Invalid payment notification"); }
}
