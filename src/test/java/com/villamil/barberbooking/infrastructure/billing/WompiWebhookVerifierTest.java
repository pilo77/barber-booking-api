package com.villamil.barberbooking.infrastructure.billing;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;

class WompiWebhookVerifierTest {
    ObjectMapper mapper = new ObjectMapper();
    WompiConfiguration config = new WompiConfiguration(true, "test", "pub_test_fixture", "prv_test_fixture", "test_integrity_fixture", "test_events_fixture", "https://example.test/billing");
    WompiTransactionLookup lookup = mock(WompiTransactionLookup.class);
    WompiWebhookVerifier verifier = new WompiWebhookVerifier(config, lookup);
    ObjectNode event() {
        ObjectNode event = mapper.createObjectNode(); event.put("event", "transaction.updated"); event.put("environment", "test");
        event.put("timestamp", Instant.now().getEpochSecond());
        var transaction = event.putObject("data").putObject("transaction");
        transaction.put("id", "test-transaction-1"); transaction.put("status", "APPROVED"); transaction.put("amount_in_cents", 500000);
        transaction.put("currency", "COP"); transaction.put("reference", "6758c141-4918-4f5b-a3c6-032a80c2a72d");
        var signature = event.putObject("signature"); signature.putArray("properties").add("transaction.id").add("transaction.status").add("transaction.amount_in_cents");
        signature.put("checksum", WompiCheckoutAdapter.sha256("test-transaction-1APPROVED500000" + event.path("timestamp").asText() + config.eventsSecret));
        when(lookup.find("test-transaction-1")).thenReturn(transaction.deepCopy()); return event;
    }
    @Test void validSignatureAndProviderConfirmationProducePayment() { assertEquals(500000, verifier.verify(event()).orElseThrow().amountInCents()); }
    @Test void forgedSignatureRejectedBeforeProviderLookup() {
        var event = event(); ((ObjectNode) event.path("signature")).put("checksum", "0".repeat(64));
        assertThrows(ForbiddenOperationException.class, () -> verifier.verify(event)); verifyNoInteractions(lookup);
    }
    @Test void unsignedReferenceTamperingRejectedByProviderLookup() {
        var event = event(); ((ObjectNode) event.path("data").path("transaction")).put("reference", "ceb263bf-2b19-4de9-b9fa-5b0ac3a6235f");
        assertThrows(ForbiddenOperationException.class, () -> verifier.verify(event));
    }
    @Test void wrongEnvironmentRejected() { var event = event(); event.put("environment", "prod"); assertThrows(ForbiddenOperationException.class, () -> verifier.verify(event)); }
    @Test void staleNotificationRejected() { var event = event(); event.put("timestamp", Instant.now().minusSeconds(172801).getEpochSecond()); assertThrows(ForbiddenOperationException.class, () -> verifier.verify(event)); }
    @Test void unsignedAmountNotAccepted() {
        var event = event(); ((ObjectNode) event.path("signature")).putArray("properties").add("transaction.id");
        ((ObjectNode) event.path("signature")).put("checksum", WompiCheckoutAdapter.sha256("test-transaction-1" + event.path("timestamp").asText() + config.eventsSecret));
        assertThrows(ForbiddenOperationException.class, () -> verifier.verify(event));
    }
}
