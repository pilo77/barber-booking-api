package com.villamil.barberbooking.application.port.out;

import java.util.Optional;
import com.villamil.barberbooking.domain.capabilities.CompanyCapabilityStatus;

public interface CapabilityRepositoryPort {
    Optional<CompanyCapabilityStatus> findStatus(Long companyId, Long branchId);

    Optional<String> findPublicationState(Long companyId, Long branchId);

    /** Holds a shared publication lock until the current booking transaction finishes. */
    Optional<String> lockPublicationState(Long companyId, Long branchId);
}
