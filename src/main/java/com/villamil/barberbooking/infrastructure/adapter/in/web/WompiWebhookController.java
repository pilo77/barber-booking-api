package com.villamil.barberbooking.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.villamil.barberbooking.application.port.in.BillingUseCase;
import com.villamil.barberbooking.infrastructure.billing.WompiWebhookVerifier;

@RestController
public class WompiWebhookController {
    private final WompiWebhookVerifier verifier;
    private final BillingUseCase billing;
    public WompiWebhookController(WompiWebhookVerifier verifier, BillingUseCase billing) {
        this.verifier = verifier; this.billing = billing;
    }
    @PostMapping("/api/v1/webhooks/wompi")
    public ResponseEntity<Void> receive(@RequestBody JsonNode event) {
        verifier.verify(event).ifPresent(billing::confirm);
        return ResponseEntity.ok().build();
    }
}
