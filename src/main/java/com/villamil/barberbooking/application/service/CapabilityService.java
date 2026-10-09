package com.villamil.barberbooking.application.service;

import java.time.Clock;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.villamil.barberbooking.application.dto.response.CompanyCapabilitiesResponse;
import com.villamil.barberbooking.application.port.in.CompanyCapabilitiesUseCase;
import com.villamil.barberbooking.application.port.out.CapabilityRepositoryPort;
import com.villamil.barberbooking.domain.capabilities.CompanyCapabilities;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;
import com.villamil.barberbooking.domain.model.Role;

@Service
public class CapabilityService implements CompanyCapabilitiesUseCase {
    private static final Set<Role> OPERATIONAL_ROLES = Set.of(Role.COMPANY_OWNER,
            Role.BRANCH_MANAGER, Role.RECEPTIONIST, Role.BARBER);
    private final CapabilityRepositoryPort repository;
    private final CurrentUserResolver users;
    private final Clock clock;

    CapabilityService(CapabilityRepositoryPort repository, CurrentUserResolver users, Clock clock) {
        this.repository = repository;
        this.users = users;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyCapabilitiesResponse currentCapabilities() {
        var user = users.requireCurrentUser();
        if (user.companyId() == null || user.branchId() == null
                || user.roles().stream().noneMatch(OPERATIONAL_ROLES::contains)) {
            throw new ForbiddenOperationException("An operational company account is required");
        }
        return CompanyCapabilitiesResponse.from(forTenant(user.companyId(), user.branchId()));
    }

    @Transactional(readOnly = true)
    public CompanyCapabilities forTenant(Long companyId, Long branchId) {
        if (companyId == null || branchId == null) {
            return CompanyCapabilities.unavailable();
        }
        return repository.findStatus(companyId, branchId)
                .map(status -> CompanyCapabilities.evaluate(status, clock.instant()))
                .orElseGet(CompanyCapabilities::unavailable);
    }
}
