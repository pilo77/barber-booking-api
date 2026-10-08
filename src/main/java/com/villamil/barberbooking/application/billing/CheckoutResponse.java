package com.villamil.barberbooking.application.billing;

public record CheckoutResponse(String reference, long amountInCents, String currency,
        String environment, String status, String checkoutUrl) { }
