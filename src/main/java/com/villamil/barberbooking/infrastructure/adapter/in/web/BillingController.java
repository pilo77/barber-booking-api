package com.villamil.barberbooking.infrastructure.adapter.in.web;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import com.villamil.barberbooking.application.billing.CheckoutResponse;
import com.villamil.barberbooking.application.billing.SubscriptionResponse;
import com.villamil.barberbooking.application.port.in.BillingUseCase;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {
    private final BillingUseCase billing;
    public BillingController(BillingUseCase billing) { this.billing = billing; }
    @GetMapping("/subscription") public SubscriptionResponse subscription() { return billing.currentSubscription(); }
    @PostMapping("/checkout") public CheckoutResponse checkout(@RequestHeader("Idempotency-Key") String key) {
        return billing.checkout(key);
    }
    @GetMapping("/orders/{reference}") public CheckoutResponse order(@PathVariable UUID reference) {
        return billing.order(reference.toString());
    }
}
