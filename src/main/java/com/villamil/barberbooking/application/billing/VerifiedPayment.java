package com.villamil.barberbooking.application.billing;

/** Issued only by the trusted provider adapter after signature and API verification. */
public record VerifiedPayment(String reference, String transactionId, long amountInCents,
        String currency, String environment, String status) { }
