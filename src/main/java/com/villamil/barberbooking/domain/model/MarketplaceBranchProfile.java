package com.villamil.barberbooking.domain.model;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;

public record MarketplaceBranchProfile(Long companyId, Long branchId, MarketplacePublicationState publicationState,
        String city, String sector, String address, String description, String contactPhone, String coverImageUrl,
        Instant submittedAt, Instant reviewedAt, Long reviewedBy, String reviewReason, Instant updatedAt) {

    public MarketplaceBranchProfile {
        if (companyId == null || companyId <= 0 || branchId == null || branchId <= 0 || publicationState == null)
            throw new BusinessRuleException("An assigned company and branch are required");
        city = text(city, 120); sector = text(sector, 120); address = text(address, 240);
        description = text(description, 2000); contactPhone = text(contactPhone, 30);
        coverImageUrl = imageUrl(coverImageUrl); reviewReason = text(reviewReason, 500);
        if (contactPhone != null && (!contactPhone.matches("[+0-9() .-]{7,30}") || contactPhone.replaceAll("[^0-9]", "").length() < 7))
            throw new BusinessRuleException("A valid public contact phone is required");
        if (publicationState == MarketplacePublicationState.PUBLISHED && (city == null || address == null
                || description == null || contactPhone == null || coverImageUrl == null || reviewedAt == null || reviewedBy == null))
            throw new BusinessRuleException("A published profile requires complete reviewed information");
        if (reviewedBy != null && reviewedBy <= 0) throw new BusinessRuleException("A valid reviewer is required");
        updatedAt = (updatedAt == null ? Instant.now() : updatedAt).truncatedTo(ChronoUnit.MICROS);
    }

    public static MarketplaceBranchProfile draft(Long company, Long branch) {
        return new MarketplaceBranchProfile(company, branch, MarketplacePublicationState.DRAFT,
                null, null, null, null, null, null, null, null, null, null, Instant.now());
    }

    public MarketplaceBranchProfile edit(String city, String sector, String address, String description, String phone, String cover) {
        if (publicationState == MarketplacePublicationState.SUSPENDED)
            throw new BusinessRuleException("A suspended publication requires platform review");
        return new MarketplaceBranchProfile(companyId, branchId, MarketplacePublicationState.DRAFT,
                city, sector, address, description, phone, cover, null, null, null, null, Instant.now());
    }

    public void requireComplete() {
        if (city == null || address == null || description == null || contactPhone == null || coverImageUrl == null)
            throw new BusinessRuleException("City, address, description, phone and an HTTPS cover image are required for publication");
    }

    public MarketplaceBranchProfile submit() {
        if (publicationState != MarketplacePublicationState.DRAFT && publicationState != MarketplacePublicationState.HIDDEN)
            throw new BusinessRuleException("Only a draft or hidden profile can be submitted");
        requireComplete();
        return state(MarketplacePublicationState.PENDING_REVIEW, Instant.now(), null, null, null);
    }

    public MarketplaceBranchProfile review(boolean approve, Long reviewer, String reason) {
        if (publicationState != MarketplacePublicationState.PENDING_REVIEW)
            throw new BusinessRuleException("This profile is no longer awaiting review");
        String explanation = text(reason, 500);
        if (!approve && explanation == null) throw new BusinessRuleException("A rejection reason is required");
        if (approve) requireComplete();
        return state(approve ? MarketplacePublicationState.PUBLISHED : MarketplacePublicationState.DRAFT,
                submittedAt, Instant.now(), reviewer, explanation);
    }

    public MarketplaceBranchProfile hide() {
        if (publicationState == MarketplacePublicationState.SUSPENDED)
            throw new BusinessRuleException("A suspended publication requires platform review");
        return state(MarketplacePublicationState.HIDDEN, submittedAt, reviewedAt, reviewedBy, reviewReason);
    }

    private MarketplaceBranchProfile state(MarketplacePublicationState state, Instant submitted, Instant reviewed, Long reviewer, String reason) {
        return new MarketplaceBranchProfile(companyId, branchId, state, city, sector, address, description,
                contactPhone, coverImageUrl, submitted, reviewed, reviewer, reason, Instant.now());
    }

    private static String text(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String clean = value.strip();
        if (clean.length() > max || clean.chars().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t'))
            throw new BusinessRuleException("Public profile text is invalid or too long");
        return clean;
    }

    private static String imageUrl(String value) {
        String clean = text(value, 500);
        if (clean == null) return null;
        try {
            URI uri = URI.create(clean);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!"https".equals(uri.getScheme()) || uri.getUserInfo() != null || uri.getFragment() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443) || !host.contains(".") || host.endsWith(".")
                    || host.matches("[0-9.]+") || host.contains(":") || host.endsWith(".localhost")
                    || host.endsWith(".local") || host.endsWith(".internal"))
                throw new IllegalArgumentException();
            String normalized = uri.toASCIIString();
            if (normalized.length() > 500) throw new IllegalArgumentException();
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("The cover must be a public HTTPS image URL without credentials");
        }
    }
}
