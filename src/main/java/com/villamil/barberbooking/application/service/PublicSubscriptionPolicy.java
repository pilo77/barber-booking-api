package com.villamil.barberbooking.application.service;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.villamil.barberbooking.application.port.out.BillingRepositoryPort;
import com.villamil.barberbooking.domain.exception.PublicResourceNotFoundException;

@Component
class PublicSubscriptionPolicy {
    private final BillingRepositoryPort billing;
    private final boolean enforced;
    PublicSubscriptionPolicy(BillingRepositoryPort billing, @Value("${billing.enforce-subscription:false}") boolean enforced) {
        this.billing = billing; this.enforced = enforced;
    }
    void requireActive(Long companyId) {
        if (enforced && !billing.subscription(companyId).activeAt(Instant.now()))
            throw new PublicResourceNotFoundException("Public booking is unavailable");
    }
}
