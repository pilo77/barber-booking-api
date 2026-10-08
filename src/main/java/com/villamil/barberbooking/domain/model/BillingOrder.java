package com.villamil.barberbooking.domain.model;

import java.time.Instant;

public record BillingOrder(String reference, Long companyId, String idempotencyKey,
        long amountInCents, String currency, String environment, String status,
        String providerTransactionId, Instant createdAt, Instant paidAt, String paymentMethod) {
    public BillingOrder(String reference, Long companyId, String idempotencyKey,
            long amountInCents, String currency, String environment, String status,
            String providerTransactionId, Instant createdAt, Instant paidAt) {
        this(reference, companyId, idempotencyKey, amountInCents, currency, environment, status,
                providerTransactionId, createdAt, paidAt, "WOMPI");
    }
    public boolean paid() { return "PAID".equals(status); }
}
