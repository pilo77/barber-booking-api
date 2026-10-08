package com.villamil.barberbooking.application.port.out;

import com.villamil.barberbooking.domain.model.BillingOrder;

public interface PaymentCheckoutPort {
    boolean enabled();
    String environment();
    String checkoutUrl(BillingOrder order);
}
