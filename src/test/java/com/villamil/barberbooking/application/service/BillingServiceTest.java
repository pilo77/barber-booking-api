package com.villamil.barberbooking.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.villamil.barberbooking.application.billing.VerifiedPayment;
import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.exception.BillingUnavailableException;
import com.villamil.barberbooking.application.exception.IdempotencyConflictException;
import com.villamil.barberbooking.application.port.out.*;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;
import com.villamil.barberbooking.domain.model.*;

class BillingServiceTest {
    BillingRepositoryPort repository = mock(BillingRepositoryPort.class);
    PaymentCheckoutPort gateway = mock(PaymentCheckoutPort.class);
    CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    BillingService service;
    String reference = "6758c141-4918-4f5b-a3c6-032a80c2a72d";
    BillingOrder pending = new BillingOrder(reference, 1L, "checkout-key-12345", 500000, "COP", "test", "PENDING", null, Instant.now(), null);
    @BeforeEach void setUp() {
        service = new BillingService(repository, gateway, new CurrentUserResolver(currentUser), 500000);
        when(gateway.enabled()).thenReturn(true); when(gateway.environment()).thenReturn("test");
        when(repository.lockOrder(reference)).thenReturn(Optional.of(pending));
        when(currentUser.currentUser()).thenReturn(Optional.of(new AuthenticatedUserResponse(1L, "owner@example.test", "Owner", 1L, 1L, null, Set.of(Role.COMPANY_OWNER))));
    }
    VerifiedPayment payment(String status) { return new VerifiedPayment(reference, "transaction-1", 500000, "COP", "test", status); }
    @Test void approvedPaymentRenewsOneCalendarMonth() {
        when(repository.lockSubscription(1L)).thenReturn(new CompanySubscription(1L, null));
        service.confirm(payment("APPROVED"));
        verify(repository).markPaid(eq(reference), eq("transaction-1"), any());
        verify(repository).renew(eq(1L), argThat(until -> until.isAfter(Instant.now().plusSeconds(27 * 86400))));
    }
    @ParameterizedTest @ValueSource(strings = {"PENDING", "DECLINED", "ERROR", "VOIDED"})
    void nonApprovedDoesNotActivate(String status) { service.confirm(payment(status)); verify(repository, never()).renew(anyLong(), any()); }
    @Test void duplicateWebhookDoesNotExtendSubscriptionAgain() {
        when(repository.lockOrder(reference)).thenReturn(Optional.of(new BillingOrder(reference, 1L, pending.idempotencyKey(), 500000, "COP", "test", "PAID", "transaction-1", Instant.now(), Instant.now())));
        service.confirm(payment("APPROVED")); verify(repository, never()).renew(anyLong(), any());
    }
    @Test void wrongAmountRejected() {
        assertThrows(IdempotencyConflictException.class, () -> service.confirm(new VerifiedPayment(reference, "transaction-1", 499999, "COP", "test", "APPROVED")));
        verify(repository, never()).markPaid(anyString(), anyString(), any());
    }
    @Test void productionEventCannotActivateSandboxOrder() {
        assertThrows(IdempotencyConflictException.class, () -> service.confirm(new VerifiedPayment(reference, "transaction-1", 500000, "COP", "prod", "APPROVED")));
    }
    @Test void disabledGatewayCannotCreateOrders() {
        when(gateway.enabled()).thenReturn(false); assertThrows(BillingUnavailableException.class, () -> service.checkout("checkout-key-12345"));
        verify(repository, never()).createOrGetOrder(anyLong(), anyString(), anyLong(), anyString());
    }
    @Test void checkoutUsesPriceAndTenantFromServer() {
        when(repository.createOrGetOrder(1L, "checkout-key-12345", 500000, "test")).thenReturn(pending);
        assertEquals(500000, service.checkout("checkout-key-12345").amountInCents());
        verify(repository).createOrGetOrder(1L, "checkout-key-12345", 500000, "test");
    }
    @Test void receptionistCannotManageBilling() {
        when(currentUser.currentUser()).thenReturn(Optional.of(new AuthenticatedUserResponse(2L, "desk@example.test", "Desk", 1L, 1L, null, Set.of(Role.RECEPTIONIST))));
        assertThrows(ForbiddenOperationException.class, () -> service.checkout("checkout-key-12345"));
    }
    @Test void orderLookupAlwaysScopedToCurrentCompany() { when(repository.findOrder(reference, 1L)).thenReturn(Optional.of(pending)); service.order(reference); verify(repository).findOrder(reference, 1L); }
    @Test void newOrdersUseUpdatedTariffFromServer() {
        service = new BillingService(repository, gateway, new CurrentUserResolver(currentUser), 4000000);
        var updated = new BillingOrder(reference, 1L, pending.idempotencyKey(), 4000000, "COP", "test", "PENDING", null, Instant.now(), null);
        when(repository.createOrGetOrder(1L, pending.idempotencyKey(), 4000000, "test")).thenReturn(updated);
        assertEquals(4000000, service.checkout(pending.idempotencyKey()).amountInCents());
        verify(repository).createOrGetOrder(1L, pending.idempotencyKey(), 4000000, "test");
    }
    @Test void tariffChangeDoesNotRepriceExistingPaymentOrder() {
        service = new BillingService(repository, gateway, new CurrentUserResolver(currentUser), 4000000);
        when(repository.lockSubscription(1L)).thenReturn(new CompanySubscription(1L, null));
        service.confirm(payment("APPROVED"));
        verify(repository).markPaid(eq(reference), eq("transaction-1"), any());
        verify(repository).renew(eq(1L), any());
    }
    @Test void manualIdempotencyKeyCannotProduceGatewayCheckout() {
        var manual = order("MANUAL", "PENDING");
        when(repository.createOrGetOrder(1L, pending.idempotencyKey(), 500000, "test")).thenReturn(manual);
        assertThrows(IdempotencyConflictException.class, () -> service.checkout(pending.idempotencyKey()));
        verify(gateway, never()).checkoutUrl(any());
    }
    @Test void manualOrderLookupCannotProduceGatewayCheckout() {
        when(repository.findOrder(reference, 1L)).thenReturn(Optional.of(order("MANUAL", "PENDING")));
        assertThrows(IdempotencyConflictException.class, () -> service.order(reference));
        verify(gateway, never()).checkoutUrl(any());
    }
    @ParameterizedTest @ValueSource(strings={"PENDING", "PAID", "REJECTED"})
    void gatewayCannotConfirmAnyManualOrder(String status) {
        when(repository.lockOrder(reference)).thenReturn(Optional.of(order("MANUAL", status)));
        assertThrows(IdempotencyConflictException.class, () -> service.confirm(payment("APPROVED")));
        verify(repository, never()).markPaid(anyString(), anyString(), any());
        verify(repository, never()).lockSubscription(anyLong());
        verify(repository, never()).renew(anyLong(), any());
    }
    @Test void rejectedGatewayOrderCannotBePaidOrReopened() {
        var rejected = order("WOMPI", "REJECTED");
        when(repository.lockOrder(reference)).thenReturn(Optional.of(rejected));
        assertThrows(IdempotencyConflictException.class, () -> service.confirm(payment("APPROVED")));
        when(repository.createOrGetOrder(1L, pending.idempotencyKey(), 500000, "test")).thenReturn(rejected);
        assertThrows(IdempotencyConflictException.class, () -> service.checkout(pending.idempotencyKey()));
        verify(repository, never()).markPaid(anyString(), anyString(), any());
        verify(repository, never()).renew(anyLong(), any());
        verify(gateway, never()).checkoutUrl(any());
    }
    private BillingOrder order(String method, String status) {
        return new BillingOrder(reference, 1L, pending.idempotencyKey(), 500000, "COP", "test", status,
                "PAID".equals(status) ? "transaction-1" : null, Instant.now(),
                "PAID".equals(status) ? Instant.now() : null, method);
    }
}
