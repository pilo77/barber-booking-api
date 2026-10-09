package com.villamil.barberbooking.application.dto.response;

import java.util.List;

public record PublicMarketplacePageResponse(List<PublicMarketplaceItemResponse> items, int page, int size, boolean hasNext) { }
