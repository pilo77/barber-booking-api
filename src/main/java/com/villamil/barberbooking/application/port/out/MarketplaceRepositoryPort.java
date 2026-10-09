package com.villamil.barberbooking.application.port.out;

import java.util.List;
import java.util.Optional;
import com.villamil.barberbooking.application.dto.response.*;
import com.villamil.barberbooking.domain.model.*;

public interface MarketplaceRepositoryPort {
    Optional<MarketplaceBranchProfile> find(Long companyId, Long branchId);
    Optional<MarketplaceBranchProfile> findPublishedCompanyProfile(Long companyId);
    MarketplaceBranchProfile lockOrCreate(Long companyId, Long branchId);
    Optional<MarketplaceBranchProfile> lockByBranch(Long branchId);
    void save(MarketplaceBranchProfile profile, MarketplacePublicationState previous, Long actorId, Long actorCompanyId);
    boolean hasBookableResources(Long companyId, Long branchId);
    List<MarketplaceSubmissionResponse> pending();
    List<PublicMarketplaceItemResponse> search(String city, String service, String query, int offset, int limit);
    boolean isBranchPublic(Long companyId, Long branchId);
    boolean isCompanyPublic(Long companyId);
}
