package com.villamil.barberbooking.application.service;

import java.time.Clock;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.villamil.barberbooking.application.port.out.BillingRepositoryPort;
import com.villamil.barberbooking.application.port.out.CapabilityRepositoryPort;
import com.villamil.barberbooking.domain.exception.PublicResourceNotFoundException;

@Component
class PublicSubscriptionPolicy {
    private final BillingRepositoryPort billing;
    private final CapabilityRepositoryPort repository;
    private final CapabilityService capabilities;
    private final Clock clock;
    private final boolean enforced;
    PublicSubscriptionPolicy(BillingRepositoryPort billing, CapabilityRepositoryPort repository,
            CapabilityService capabilities, Clock clock,
            @Value("${billing.enforce-subscription:false}") boolean enforced) {
        this.billing = billing;
        this.repository = repository;
        this.capabilities = capabilities;
        this.clock = clock;
        this.enforced = enforced;
    }
    void requireActive(Long companyId) {
        if (enforced && !billing.subscription(companyId).activeAt(clock.instant()))
            throw new PublicResourceNotFoundException("Public booking is unavailable");
    }

    void requirePublicBooking(Long companyId, Long branchId) {
        requireActiveTenant(companyId, branchId);
        validatePublication(companyId, repository.findPublicationState(companyId, branchId));
    }

    void requirePublicBookingLocked(Long companyId, Long branchId) {
        // Creation is transactional: writers cannot hide/edit this profile until the booking commits.
        var publication = repository.lockPublicationState(companyId, branchId);
        requireActiveTenant(companyId, branchId);
        validatePublication(companyId, publication);
    }

    private void requireActiveTenant(Long companyId, Long branchId) {
        if (!capabilities.forTenant(companyId, branchId).basicBooking()) {
            throw new PublicResourceNotFoundException("Public booking is unavailable");
        }
    }

    private void validatePublication(Long companyId, Optional<String> publication) {
        if (publication.isPresent()) {
            if (!"PUBLISHED".equals(publication.get())) {
                throw new PublicResourceNotFoundException("Public booking is unavailable");
            }
            return;
        }
        // Existing private links retain their previous commercial policy until explicitly published.
        requireActive(companyId);
    }
}
