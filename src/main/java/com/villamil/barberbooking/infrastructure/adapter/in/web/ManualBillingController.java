package com.villamil.barberbooking.infrastructure.adapter.in.web;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.villamil.barberbooking.application.billing.*;
import com.villamil.barberbooking.application.port.in.ManualBillingUseCase;
@RestController @RequestMapping("/api/v1")
public class ManualBillingController {
    private final ManualBillingUseCase billing;
    public ManualBillingController(ManualBillingUseCase billing) { this.billing = billing; }
    @GetMapping("/billing/transfer-instructions") public TransferInstructions instructions() { return billing.instructions(); }
    @GetMapping("/billing/transfers") public List<ManualPaymentReport> own() { return billing.ownReports(); }
    @PostMapping("/billing/transfers") public ManualPaymentReport report(@RequestHeader("Idempotency-Key") String key, @Valid @RequestBody TransferRequest request) {
        return billing.report(key, request.transferReference());
    }
    @GetMapping("/platform/billing/transfers") public List<ManualPaymentReport> pending() { return billing.pending(); }
    @PatchMapping("/platform/billing/transfers/{reference}/approve") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void approve(@PathVariable UUID reference, @Valid @RequestBody ApprovalRequest request) {
        billing.approve(reference.toString(), request.bankTransactionId(), request.confirmedAmountInCents());
    }
    @PatchMapping("/platform/billing/transfers/{reference}/reject") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void reject(@PathVariable UUID reference, @Valid @RequestBody RejectionRequest request) { billing.reject(reference.toString(), request.reason()); }
    public record TransferRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9._:-]{6,80}") String transferReference) { }
    public record ApprovalRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9._:-]{6,80}") String bankTransactionId, @Positive long confirmedAmountInCents) { }
    public record RejectionRequest(@NotBlank @Size(max=255) String reason) { }
}
