package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.billing.CheckoutResponse;
import com.villamil.barberbooking.application.billing.SubscriptionResponse;
import com.villamil.barberbooking.application.billing.VerifiedPayment;

public interface BillingUseCase {
    SubscriptionResponse currentSubscription();
    CheckoutResponse checkout(String idempotencyKey);
    CheckoutResponse order(String reference);
    void confirm(VerifiedPayment payment);
}
