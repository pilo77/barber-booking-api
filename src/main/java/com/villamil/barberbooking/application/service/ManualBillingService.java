package com.villamil.barberbooking.application.service;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.villamil.barberbooking.application.billing.*;
import com.villamil.barberbooking.application.exception.*;
import com.villamil.barberbooking.application.port.in.ManualBillingUseCase;
import com.villamil.barberbooking.application.port.out.*;
import com.villamil.barberbooking.domain.exception.*;
import com.villamil.barberbooking.domain.model.Role;

@Service
public class ManualBillingService implements ManualBillingUseCase {
    private final ManualPaymentRepositoryPort reports;
    private final BillingRepositoryPort billing;
    private final CurrentUserResolver users;
    private final long amount;
    private final String instructions;
    ManualBillingService(ManualPaymentRepositoryPort reports, BillingRepositoryPort billing, CurrentUserResolver users,
            @Value("${billing.basic.amount-in-cents:4000000}") long amount,
            @Value("${billing.manual.instructions:}") String instructions) {
        this.reports = reports; this.billing = billing; this.users = users; this.amount = amount; this.instructions = instructions.strip();
        if (amount < 100 || amount > 100_000_000 || instructions.length() > 1000) throw new IllegalStateException("Invalid manual billing configuration");
    }
    @Override public TransferInstructions instructions() { owner(); return new TransferInstructions(!instructions.isBlank(), instructions); }
    @Override @Transactional public ManualPaymentReport report(String key, String transferReference) {
        Long company = owner();
        if (instructions.isBlank()) throw new BillingUnavailableException();
        if (key == null || !key.matches("[A-Za-z0-9_-]{16,128}") || transferReference == null || !transferReference.matches("[A-Za-z0-9._:-]{6,80}"))
            throw new BusinessRuleException("A valid idempotency key and transfer reference are required");
        var report = reports.createReport(company, users.requireCurrentUser().id(), key, amount, transferReference);
        if (!transferReference.equals(report.declaredTransferReference())) throw new IdempotencyConflictException("Transfer report payload changed");
        return report;
    }
    @Override @Transactional(readOnly=true) public List<ManualPaymentReport> ownReports() { return reports.ownReports(owner()); }
    @Override @Transactional(readOnly=true) public List<ManualPaymentReport> pending() { reviewer(); return reports.pending(); }
    @Override @Transactional public void approve(String reference, String bankTransactionId, long confirmedAmount) {
        Long reviewer = reviewer();
        if (bankTransactionId == null || !bankTransactionId.matches("[A-Za-z0-9._:-]{6,80}")) throw new BusinessRuleException("A bank transaction identifier is required");
        var report = reports.lockReport(reference).orElseThrow(() -> new PublicResourceNotFoundException("Transfer report not found"));
        var order = billing.lockOrder(reference).orElseThrow(() -> new PublicResourceNotFoundException("Payment order not found"));
        if (confirmedAmount != report.amountInCents()) throw new BusinessRuleException("Confirmed bank amount must match the order");
        if ("PAID".equals(report.status())) {
            if (!("manual:" + bankTransactionId).equals(order.providerTransactionId())) throw new IdempotencyConflictException("Order already paid by another bank transaction");
            return;
        }
        if (!"PENDING".equals(report.status())) throw new IdempotencyConflictException("A rejected report cannot be approved");
        var subscription = billing.lockSubscription(order.companyId());
        Instant now = Instant.now();
        reports.recordApproval(reference, reviewer, bankTransactionId, confirmedAmount, now);
        billing.renew(order.companyId(), subscription.renewedUntil(now));
    }
    @Override @Transactional public void reject(String reference, String reason) {
        Long reviewer = reviewer();
        if (reason == null || reason.isBlank() || reason.length() > 255) throw new BusinessRuleException("A brief rejection reason is required");
        var report = reports.lockReport(reference).orElseThrow(() -> new PublicResourceNotFoundException("Transfer report not found"));
        if ("REJECTED".equals(report.status())) return;
        if (!"PENDING".equals(report.status())) throw new IdempotencyConflictException("A paid report cannot be rejected");
        reports.recordRejection(reference, reviewer, reason.strip(), Instant.now());
    }
    private Long owner() {
        var user = users.requireCurrentUser();
        if (!user.roles().contains(Role.COMPANY_OWNER) || user.companyId() == null) throw new ForbiddenOperationException("Only a company owner may report a transfer");
        return user.companyId();
    }
    private Long reviewer() {
        var user = users.requireCurrentUser();
        if (!user.roles().contains(Role.PLATFORM_OWNER)) throw new ForbiddenOperationException("Only the platform owner may review transfers");
        return user.id();
    }
}
