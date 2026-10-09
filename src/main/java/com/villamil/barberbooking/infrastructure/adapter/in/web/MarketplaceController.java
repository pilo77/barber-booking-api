package com.villamil.barberbooking.infrastructure.adapter.in.web;

import java.util.List;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import com.villamil.barberbooking.application.dto.command.UpdateMarketplaceProfileCommand;
import com.villamil.barberbooking.application.dto.response.*;
import com.villamil.barberbooking.application.port.in.MarketplaceUseCase;

@RestController
@RequestMapping("/api/v1")
public class MarketplaceController {
    private final MarketplaceUseCase marketplace;
    public MarketplaceController(MarketplaceUseCase marketplace) { this.marketplace = marketplace; }

    @GetMapping("/company/marketplace-profile")
    public MarketplaceProfileResponse profile() { return marketplace.currentProfile(); }

    @PutMapping("/company/marketplace-profile")
    public MarketplaceProfileResponse update(@Valid @RequestBody ProfileRequest request) {
        return marketplace.update(new UpdateMarketplaceProfileCommand(request.city(), request.sector(), request.address(),
                request.description(), request.contactPhone(), request.coverImageUrl()));
    }

    @PostMapping("/company/marketplace-profile/submit")
    public MarketplaceProfileResponse submit() { return marketplace.submit(); }

    @PostMapping("/company/marketplace-profile/hide")
    public MarketplaceProfileResponse hide() { return marketplace.hide(); }

    @GetMapping("/platform/marketplace/submissions")
    public List<MarketplaceSubmissionResponse> pending() { return marketplace.pending(); }

    @PatchMapping("/platform/marketplace/submissions/{branchId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable Long branchId, @Valid @RequestBody ReviewRequest request) {
        marketplace.review(branchId, true, request.reason(), request.expectedUpdatedAt());
    }

    @PatchMapping("/platform/marketplace/submissions/{branchId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable Long branchId, @Valid @RequestBody ReviewRequest request) {
        marketplace.review(branchId, false, request.reason(), request.expectedUpdatedAt());
    }

    @GetMapping("/public/marketplace")
    public PublicMarketplacePageResponse search(@RequestParam(required = false) String city,
            @RequestParam(required = false) String service, @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
        return marketplace.search(city, service, query, page, size);
    }

    public record ProfileRequest(@Size(max = 120) String city, @Size(max = 120) String sector,
            @Size(max = 240) String address, @Size(max = 2000) String description,
            @Size(max = 30) String contactPhone, @Size(max = 500) String coverImageUrl) { }
    public record ReviewRequest(@Size(max = 500) String reason, @NotNull Instant expectedUpdatedAt) { }
}
