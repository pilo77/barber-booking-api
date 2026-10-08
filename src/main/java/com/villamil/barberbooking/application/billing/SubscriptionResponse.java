package com.villamil.barberbooking.application.billing;

import java.time.Instant;

public record SubscriptionResponse(String plan, long amountInCents, String currency,
        String interval, boolean active, Instant validUntil, boolean checkoutEnabled,
        String paymentEnvironment) { }
