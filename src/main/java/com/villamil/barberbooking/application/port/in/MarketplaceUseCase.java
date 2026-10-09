package com.villamil.barberbooking.application.port.in;

import java.util.List;
import java.time.Instant;
import com.villamil.barberbooking.application.dto.command.UpdateMarketplaceProfileCommand;
import com.villamil.barberbooking.application.dto.response.*;

public interface MarketplaceUseCase {
    MarketplaceProfileResponse currentProfile();
    MarketplaceProfileResponse update(UpdateMarketplaceProfileCommand command);
    MarketplaceProfileResponse submit();
    MarketplaceProfileResponse hide();
    List<MarketplaceSubmissionResponse> pending();
    void review(Long branchId, boolean approve, String reason, Instant expectedUpdatedAt);
    PublicMarketplacePageResponse search(String city, String service, String query, int page, int size);
}
