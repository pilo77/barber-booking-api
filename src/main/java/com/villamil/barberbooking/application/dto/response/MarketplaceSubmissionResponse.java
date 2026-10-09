package com.villamil.barberbooking.application.dto.response;

public record MarketplaceSubmissionResponse(Long companyId, Long branchId, String companyName, String branchName,
        MarketplaceProfileResponse profile) { }
