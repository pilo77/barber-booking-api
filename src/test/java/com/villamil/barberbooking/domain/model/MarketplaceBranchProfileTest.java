package com.villamil.barberbooking.domain.model;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;

class MarketplaceBranchProfileTest {
    MarketplaceBranchProfile complete() {
        return MarketplaceBranchProfile.draft(7L, 9L).edit("Neiva", "Centro", "Calle 1 # 2-3",
                "Barbería profesional con atención mediante reserva.", "3001234567", "https://cdn.example.test/cover.jpg");
    }

    @Test void publicationNeedsCompleteProfileAndReview() {
        assertThrows(BusinessRuleException.class, () -> MarketplaceBranchProfile.draft(7L, 9L).submit());
        var pending = complete().submit();
        assertEquals(MarketplacePublicationState.PENDING_REVIEW, pending.publicationState());
        assertNotNull(pending.submittedAt());
        var published = pending.review(true, 100L, "Verified public business information");
        assertEquals(MarketplacePublicationState.PUBLISHED, published.publicationState());
        assertEquals(100L, published.reviewedBy());
        assertThrows(BusinessRuleException.class, () -> published.review(true, 100L, null));
    }

    @Test void publishedAndPendingEditsRevokePublicationAndRequireFreshReview() {
        var published = complete().submit().review(true, 100L, null);
        var edited = published.edit("Bogotá", null, "Nueva dirección", "Descripción corregida", "3001234567", "https://cdn.example.test/new.jpg");
        assertEquals(MarketplacePublicationState.DRAFT, edited.publicationState());
        assertNull(edited.reviewedAt()); assertNull(edited.submittedAt());
        assertThrows(BusinessRuleException.class, () -> edited.review(true, 100L, null));
        var pendingEdit = complete().submit().edit("Neiva", null, "Otra dirección", "Nueva descripción", "3001234567", null);
        assertEquals(MarketplacePublicationState.DRAFT, pendingEdit.publicationState());
    }

    @Test void rejectionNeedsReasonAndHiddenProfileMayBeResubmitted() {
        assertThrows(BusinessRuleException.class, () -> complete().submit().review(false, 100L, " "));
        var rejected = complete().submit().review(false, 100L, "Please correct the public address");
        assertEquals(MarketplacePublicationState.DRAFT, rejected.publicationState());
        assertEquals("Please correct the public address", rejected.reviewReason());
        assertEquals(MarketplacePublicationState.PENDING_REVIEW, complete().hide().submit().publicationState());
    }

    @Test void ownerCannotCircumventSuspension() {
        var suspended = new MarketplaceBranchProfile(7L, 9L, MarketplacePublicationState.SUSPENDED,
                null, null, null, null, null, null, null, null, null, null, Instant.now());
        assertThrows(BusinessRuleException.class, suspended::submit);
        assertThrows(BusinessRuleException.class, suspended::hide);
        assertThrows(BusinessRuleException.class, () -> suspended.edit(null, null, null, null, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "http://cdn.example.test/a.jpg", "javascript:alert(1)", "https://user:password@cdn.example.test/a.jpg",
            "https://localhost/a.jpg", "https://127.0.0.1/a.jpg", "https://10.1.2.3/a.jpg", "https://[::1]/a.jpg",
            "https://cdn.internal/a.jpg", "https://cdn.local/a.jpg", "https://cdn.example.test:8080/a.jpg", "https://cdn.example.test/a.jpg#fragment" })
    void rejectsUnsafeCoverURLs(String cover) {
        assertThrows(BusinessRuleException.class, () -> complete().edit("Neiva", null, "Address", "Description", "3001234567", cover));
    }

    @Test void validatesDomainFieldsBeyondController() {
        assertThrows(BusinessRuleException.class, () -> complete().edit("x".repeat(121), null, null, null, null, null));
        assertThrows(BusinessRuleException.class, () -> complete().edit(null, null, null, "bad\u0000value", null, null));
        assertThrows(BusinessRuleException.class, () -> complete().edit(null, null, null, null, "not a phone", null));
    }
}
