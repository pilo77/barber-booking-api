package com.villamil.barberbooking.application.dto.response;

import java.math.BigDecimal;

public record PublicMarketplaceItemResponse(String companySlug, String branchSlug, String companyName, String branchName,
        String city, String sector, String address, String description, String contactPhone, String coverImageUrl,
        BigDecimal startingPrice) { }
