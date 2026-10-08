package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import com.villamil.barberbooking.application.billing.ManualPaymentReport;
import com.villamil.barberbooking.application.port.out.ManualPaymentRepositoryPort;
@Component
public class ManualPaymentPersistenceAdapter implements ManualPaymentRepositoryPort {
    private final JdbcTemplate jdbc;
    private static final String SELECT = "SELECT o.*, c.name AS company_name, u.email AS reporter_email FROM subscription_payment_orders o JOIN companies c ON c.id = o.company_id LEFT JOIN user_accounts u ON u.id = o.reported_by_user_id AND u.company_id = o.company_id ";
    private final RowMapper<ManualPaymentReport> mapper = (rs, row) -> new ManualPaymentReport(rs.getString("reference"), rs.getString("company_name"), rs.getString("reporter_email"),
      rs.getLong("amount_in_cents"), rs.getString("status"), rs.getString("declared_transfer_reference"), rs.getTimestamp("created_at").toInstant(), rs.getString("rejection_reason"));
    public ManualPaymentPersistenceAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public ManualPaymentReport createReport(Long company, Long reporter, String key, long amount, String transferReference) {
        jdbc.update("""
          INSERT INTO subscription_payment_orders(reference, company_id, reported_by_user_id, idempotency_key, amount_in_cents, currency, environment, payment_method, declared_transfer_reference)
          VALUES (?, ?, ?, ?, ?, 'COP', 'prod', 'MANUAL', ?) ON CONFLICT(company_id, idempotency_key) DO NOTHING
          """, UUID.randomUUID(), company, reporter, key, amount, transferReference);
        return jdbc.query(SELECT + "WHERE o.company_id = ? AND o.idempotency_key = ? AND o.payment_method = 'MANUAL'", mapper, company, key)
            .stream().findFirst().orElseThrow(() -> new com.villamil.barberbooking.application.exception.IdempotencyConflictException("Key belongs to another payment method"));
    }
    @Override public Optional<ManualPaymentReport> lockReport(String reference) {
        return jdbc.query(SELECT + "WHERE o.reference = ? AND o.payment_method = 'MANUAL' FOR UPDATE OF o", mapper, UUID.fromString(reference)).stream().findFirst();
    }
    @Override public List<ManualPaymentReport> ownReports(Long company) {
        return jdbc.query(SELECT + "WHERE o.company_id = ? AND o.payment_method = 'MANUAL' ORDER BY o.created_at DESC LIMIT 100", mapper, company);
    }
    @Override public List<ManualPaymentReport> pending() {
        return jdbc.query(SELECT + "WHERE o.payment_method = 'MANUAL' AND o.status = 'PENDING' ORDER BY o.created_at LIMIT 100", mapper);
    }
    @Override public void recordApproval(String reference, Long reviewer, String transaction, long amount, Instant now) {
        // Approval, audit insertion and subscription renewal share the application transaction.
        jdbc.update("UPDATE subscription_payment_orders SET status = 'PAID', provider_transaction_id = ?, paid_at = ?, reviewed_by = ?, reviewed_at = ? WHERE reference = ?",
          "manual:" + transaction, Timestamp.from(now), reviewer, Timestamp.from(now), UUID.fromString(reference));
        jdbc.update("INSERT INTO subscription_payment_reviews(order_reference, reviewer_id, decision, bank_transaction_id, confirmed_amount_in_cents) VALUES (?, ?, 'APPROVED', ?, ?)",
          UUID.fromString(reference), reviewer, transaction, amount);
    }
    @Override public void recordRejection(String reference, Long reviewer, String reason, Instant now) {
        jdbc.update("UPDATE subscription_payment_orders SET status = 'REJECTED', reviewed_by = ?, reviewed_at = ?, rejection_reason = ? WHERE reference = ?",
          reviewer, Timestamp.from(now), reason, UUID.fromString(reference));
        jdbc.update("INSERT INTO subscription_payment_reviews(order_reference, reviewer_id, decision, reason) VALUES (?, ?, 'REJECTED', ?)", UUID.fromString(reference), reviewer, reason);
    }
}
