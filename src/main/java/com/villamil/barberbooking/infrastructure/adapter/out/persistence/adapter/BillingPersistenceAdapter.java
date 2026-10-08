package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.villamil.barberbooking.application.port.out.BillingRepositoryPort;
import com.villamil.barberbooking.domain.model.BillingOrder;
import com.villamil.barberbooking.domain.model.CompanySubscription;

@Component
public class BillingPersistenceAdapter implements BillingRepositoryPort {
    private final JdbcTemplate jdbc;
    public BillingPersistenceAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public CompanySubscription subscription(Long companyId) { return subscription(companyId, false); }
    @Override public CompanySubscription lockSubscription(Long companyId) { return subscription(companyId, true); }
    private CompanySubscription subscription(Long companyId, boolean lock) {
        return jdbc.queryForObject("SELECT company_id, valid_until FROM company_subscriptions WHERE company_id = ?"
                + (lock ? " FOR UPDATE" : ""), (rs, row) ->
                new CompanySubscription(rs.getLong("company_id"), instant(rs, "valid_until")), companyId);
    }

    @Override public BillingOrder createOrGetOrder(Long companyId, String key, long amount, String environment) {
        jdbc.update("""
                INSERT INTO subscription_payment_orders(reference, company_id, idempotency_key, amount_in_cents, currency, environment)
                VALUES (?, ?, ?, ?, 'COP', ?) ON CONFLICT (company_id, idempotency_key) DO NOTHING
                """, UUID.randomUUID(), companyId, key, amount, environment);
        return jdbc.queryForObject("SELECT * FROM subscription_payment_orders WHERE company_id = ? AND idempotency_key = ?",
                this::order, companyId, key);
    }
    @Override public Optional<BillingOrder> lockOrder(String reference) {
        return jdbc.query("SELECT * FROM subscription_payment_orders WHERE reference = ? FOR UPDATE",
                this::order, UUID.fromString(reference)).stream().findFirst();
    }
    @Override public Optional<BillingOrder> findOrder(String reference, Long companyId) {
        return jdbc.query("SELECT * FROM subscription_payment_orders WHERE reference = ? AND company_id = ?",
                this::order, UUID.fromString(reference), companyId).stream().findFirst();
    }
    @Override public void markPaid(String reference, String transactionId, Instant paidAt) {
        jdbc.update("UPDATE subscription_payment_orders SET status = 'PAID', provider_transaction_id = ?, paid_at = ? WHERE reference = ?",
                transactionId, Timestamp.from(paidAt), UUID.fromString(reference));
    }
    @Override public void renew(Long companyId, Instant validUntil) {
        jdbc.update("UPDATE company_subscriptions SET valid_until = ?, updated_at = now() WHERE company_id = ?",
                Timestamp.from(validUntil), companyId);
    }
    private BillingOrder order(ResultSet rs, int row) throws SQLException {
        return new BillingOrder(rs.getString("reference"), rs.getLong("company_id"), rs.getString("idempotency_key"),
                rs.getLong("amount_in_cents"), rs.getString("currency"), rs.getString("environment"),
                rs.getString("status"), rs.getString("provider_transaction_id"), instant(rs, "created_at"), instant(rs, "paid_at"),
                rs.getString("payment_method"));
    }
    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
