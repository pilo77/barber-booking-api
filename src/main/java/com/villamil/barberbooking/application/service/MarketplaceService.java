package com.villamil.barberbooking.application.service;

import java.util.List;
import java.time.Instant;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.villamil.barberbooking.application.dto.command.UpdateMarketplaceProfileCommand;
import com.villamil.barberbooking.application.dto.response.*;
import com.villamil.barberbooking.application.port.in.MarketplaceUseCase;
import com.villamil.barberbooking.application.port.out.MarketplaceRepositoryPort;
import com.villamil.barberbooking.application.exception.IdempotencyConflictException;
import com.villamil.barberbooking.domain.exception.*;
import com.villamil.barberbooking.domain.model.*;

@Service
class MarketplaceService implements MarketplaceUseCase {
    private final MarketplaceRepositoryPort repository;
    private final CurrentUserResolver users;
    MarketplaceService(MarketplaceRepositoryPort repository, CurrentUserResolver users) {
        this.repository = repository; this.users = users;
    }

    @Override @Transactional(readOnly = true)
    public MarketplaceProfileResponse currentProfile() {
        var owner = owner();
        return MarketplaceProfileResponse.from(repository.find(owner.companyId(), owner.branchId())
                .orElseGet(() -> MarketplaceBranchProfile.draft(owner.companyId(), owner.branchId())));
    }

    @Override @Transactional
    public MarketplaceProfileResponse update(UpdateMarketplaceProfileCommand command) {
        if (command == null) throw new BusinessRuleException("A public profile is required");
        return mutate(p -> p.edit(command.city(), command.sector(), command.address(), command.description(),
                command.contactPhone(), command.coverImageUrl()));
    }

    @Override @Transactional
    public MarketplaceProfileResponse submit() {
        return mutate(p -> {
            if (!repository.hasBookableResources(p.companyId(), p.branchId()))
                throw new BusinessRuleException("Publication needs an active online service, barber and working schedule");
            return p.submit();
        });
    }

    @Override @Transactional
    public MarketplaceProfileResponse hide() { return mutate(MarketplaceBranchProfile::hide); }

    private MarketplaceProfileResponse mutate(Function<MarketplaceBranchProfile, MarketplaceBranchProfile> change) {
        var owner = owner();
        var previous = repository.lockOrCreate(owner.companyId(), owner.branchId());
        var updated = change.apply(previous);
        repository.save(updated, previous.publicationState(), owner.id(), owner.companyId());
        return MarketplaceProfileResponse.from(updated);
    }

    @Override @Transactional(readOnly = true)
    public List<MarketplaceSubmissionResponse> pending() { admin(); return repository.pending(); }

    @Override @Transactional
    public void review(Long branchId, boolean approve, String reason, Instant expectedUpdatedAt) {
        var admin = admin();
        if (branchId == null || branchId <= 0) throw new BusinessRuleException("A valid branch is required");
        if (expectedUpdatedAt == null) throw new BusinessRuleException("The reviewed profile version is required");
        var previous = repository.lockByBranch(branchId)
                .orElseThrow(() -> new PublicResourceNotFoundException("Publication not found"));
        if (!previous.updatedAt().equals(expectedUpdatedAt))
            throw new IdempotencyConflictException("The publication changed; reload and review its current information");
        if (approve && !repository.hasBookableResources(previous.companyId(), previous.branchId()))
            throw new BusinessRuleException("Publication needs an active online service, barber and working schedule");
        var updated = previous.review(approve, admin.id(), reason);
        repository.save(updated, previous.publicationState(), admin.id(), null);
    }

    @Override @Transactional(readOnly = true)
    public PublicMarketplacePageResponse search(String city, String service, String query, int page, int size) {
        if (page < 0 || page > 10000 || size < 1 || size > 20)
            throw new BusinessRuleException("Page must be 0 to 10000 and size 1 to 20");
        var found = repository.search(filter(city), filter(service), filter(query), page * size, size + 1);
        return new PublicMarketplacePageResponse(List.copyOf(found.stream().limit(size).toList()), page, size, found.size() > size);
    }

    private String filter(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.length() > 120 || value.chars().anyMatch(Character::isISOControl))
            throw new BusinessRuleException("Search filters must contain at most 120 characters");
        return value.strip();
    }

    private AuthenticatedUserResponse owner() {
        var actor = users.requireCurrentUser();
        if (!actor.roles().contains(Role.COMPANY_OWNER) || actor.companyId() == null || actor.branchId() == null)
            throw new ForbiddenOperationException("Only the assigned company owner may manage publication");
        return actor;
    }

    private AuthenticatedUserResponse admin() {
        var actor = users.requireCurrentUser();
        if (!actor.roles().contains(Role.PLATFORM_OWNER))
            throw new ForbiddenOperationException("Only the platform owner may review publication");
        return actor;
    }
}
