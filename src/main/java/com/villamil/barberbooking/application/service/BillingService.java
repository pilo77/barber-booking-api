package com.villamil.barberbooking.application.service;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.villamil.barberbooking.application.billing.*;
import com.villamil.barberbooking.application.exception.BillingUnavailableException;
import com.villamil.barberbooking.application.exception.IdempotencyConflictException;
import com.villamil.barberbooking.application.port.in.BillingUseCase;
import com.villamil.barberbooking.application.port.out.BillingRepositoryPort;
import com.villamil.barberbooking.application.port.out.PaymentCheckoutPort;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;
import com.villamil.barberbooking.domain.exception.PublicResourceNotFoundException;
import com.villamil.barberbooking.domain.model.BillingOrder;
import com.villamil.barberbooking.domain.model.Role;

@Service
public class BillingService implements BillingUseCase {
    private final BillingRepositoryPort repository;
    private final PaymentCheckoutPort gateway;
    private final CurrentUserResolver users;
    private final long amount;

    BillingService(BillingRepositoryPort repository, PaymentCheckoutPort gateway,
            CurrentUserResolver users, @Value("${billing.basic.amount-in-cents:4000000}") long amount) {
        if (amount < 100 || amount > 100_000_000) throw new IllegalStateException("Invalid basic plan price");
        this.repository = repository;
        this.gateway = gateway;
        this.users = users;
        this.amount = amount;
    }

    @Override @Transactional(readOnly = true)
    public SubscriptionResponse currentSubscription() {
        var subscription = repository.subscription(company());
        return new SubscriptionResponse("BASIC", amount, "COP", "MONTHLY",
                subscription.activeAt(Instant.now()), subscription.validUntil(), gateway.enabled(), gateway.environment());
    }

    @Override @Transactional
    public CheckoutResponse checkout(String key) {
        if (!gateway.enabled()) throw new BillingUnavailableException();
        if (key == null || !key.matches("[A-Za-z0-9_-]{16,128}"))
            throw new BusinessRuleException("A valid Idempotency-Key is required");
        BillingOrder order = repository.createOrGetOrder(company(), key, amount, gateway.environment());
        if (!order.environment().equals(gateway.environment()))
            throw new IdempotencyConflictException("Idempotency key belongs to a different payment environment");
        return response(order);
    }

    @Override @Transactional(readOnly = true)
    public CheckoutResponse order(String reference) {
        return response(repository.findOrder(reference, company())
                .orElseThrow(() -> new PublicResourceNotFoundException("Payment order not found")));
    }

    @Override @Transactional
    public void confirm(VerifiedPayment payment) {
        if (!gateway.enabled()) throw new BillingUnavailableException();
        BillingOrder order = repository.lockOrder(payment.reference())
                .orElseThrow(() -> new PublicResourceNotFoundException("Payment order not found"));
        requireGatewayOrder(order);
        if (!order.environment().equals(payment.environment())
                || !gateway.environment().equals(payment.environment())
                || order.amountInCents() != payment.amountInCents()
                || !order.currency().equals(payment.currency()))
            throw new IdempotencyConflictException("Payment does not match order");
        if (!"APPROVED".equals(payment.status())) return;
        if (order.paid()) {
            if (!payment.transactionId().equals(order.providerTransactionId()))
                throw new IdempotencyConflictException("Order already paid by another transaction");
            return;
        }
        var subscription = repository.lockSubscription(order.companyId());
        Instant now = Instant.now();
        repository.markPaid(order.reference(), payment.transactionId(), now);
        repository.renew(order.companyId(), subscription.renewedUntil(now));
    }

    private CheckoutResponse response(BillingOrder order) {
        requireGatewayOrder(order);
        return new CheckoutResponse(order.reference(), order.amountInCents(), order.currency(),
                order.environment(), order.status(), order.paid() ? null : gateway.checkoutUrl(order));
    }

    private void requireGatewayOrder(BillingOrder order) {
        if (!"WOMPI".equals(order.paymentMethod()))
            throw new IdempotencyConflictException("Order belongs to a different payment method");
        if (!"PENDING".equals(order.status()) && !"PAID".equals(order.status()))
            throw new IdempotencyConflictException("Order is not available for gateway payment");
    }

    private Long company() {
        var user = users.requireCurrentUser();
        if (!user.roles().contains(Role.COMPANY_OWNER) || user.companyId() == null)
            throw new ForbiddenOperationException("Only a company owner may manage its subscription");
        return user.companyId();
    }
}
