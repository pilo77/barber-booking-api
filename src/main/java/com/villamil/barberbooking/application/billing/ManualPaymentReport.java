package com.villamil.barberbooking.application.billing;
import java.time.Instant;
public record ManualPaymentReport(String reference, String companyName, String reporterEmail, long amountInCents, String status,
        String declaredTransferReference, Instant createdAt, String rejectionReason) { }
