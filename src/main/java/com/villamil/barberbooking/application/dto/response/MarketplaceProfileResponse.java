package com.villamil.barberbooking.application.dto.response;

import java.time.Instant;
import com.villamil.barberbooking.domain.model.MarketplaceBranchProfile;
import com.villamil.barberbooking.domain.model.MarketplacePublicationState;

public record MarketplaceProfileResponse(MarketplacePublicationState publicationState, String city, String sector,
        String address, String description, String contactPhone, String coverImageUrl,
        Instant submittedAt, Instant reviewedAt, String reviewReason, Instant updatedAt) {
    public static MarketplaceProfileResponse from(MarketplaceBranchProfile p) {
        return new MarketplaceProfileResponse(p.publicationState(), p.city(), p.sector(), p.address(), p.description(),
                p.contactPhone(), p.coverImageUrl(), p.submittedAt(), p.reviewedAt(), p.reviewReason(), p.updatedAt());
    }
}
