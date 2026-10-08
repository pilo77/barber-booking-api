package com.villamil.barberbooking.application.port.out;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.villamil.barberbooking.application.billing.ManualPaymentReport;
public interface ManualPaymentRepositoryPort {
    ManualPaymentReport createReport(Long companyId, Long reporterId, String key, long amount, String transferReference);
    Optional<ManualPaymentReport> lockReport(String reference);
    List<ManualPaymentReport> ownReports(Long companyId);
    List<ManualPaymentReport> pending();
    void recordApproval(String reference, Long reviewerId, String bankTransactionId, long amount, Instant now);
    void recordRejection(String reference, Long reviewerId, String reason, Instant now);
}
