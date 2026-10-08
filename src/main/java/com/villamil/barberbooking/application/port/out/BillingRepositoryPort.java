package com.villamil.barberbooking.application.port.out;

import java.time.Instant;
import java.util.Optional;
import com.villamil.barberbooking.domain.model.BillingOrder;
import com.villamil.barberbooking.domain.model.CompanySubscription;

public interface BillingRepositoryPort {
    CompanySubscription subscription(Long companyId);
    CompanySubscription lockSubscription(Long companyId);
    BillingOrder createOrGetOrder(Long companyId, String key, long amount, String environment);
    Optional<BillingOrder> lockOrder(String reference);
    Optional<BillingOrder> findOrder(String reference, Long companyId);
    void markPaid(String reference, String transactionId, Instant paidAt);
    void renew(Long companyId, Instant validUntil);
}
