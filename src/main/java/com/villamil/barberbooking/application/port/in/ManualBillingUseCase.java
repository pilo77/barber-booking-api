package com.villamil.barberbooking.application.port.in;
import java.util.List;
import com.villamil.barberbooking.application.billing.*;
public interface ManualBillingUseCase {
    TransferInstructions instructions();
    ManualPaymentReport report(String key, String transferReference);
    List<ManualPaymentReport> ownReports();
    List<ManualPaymentReport> pending();
    void approve(String reference, String bankTransactionId, long confirmedAmountInCents);
    void reject(String reference, String reason);
}
