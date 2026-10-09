package com.villamil.barberbooking.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.villamil.barberbooking.application.dto.command.UpdateMarketplaceProfileCommand;
import com.villamil.barberbooking.application.dto.response.*;
import com.villamil.barberbooking.application.port.out.*;
import com.villamil.barberbooking.application.exception.IdempotencyConflictException;
import com.villamil.barberbooking.domain.exception.*;
import com.villamil.barberbooking.domain.model.*;

class MarketplaceServiceTest {
    MarketplaceRepositoryPort repository = mock(MarketplaceRepositoryPort.class);
    CurrentUserProvider provider = mock(CurrentUserProvider.class);
    MarketplaceService service = new MarketplaceService(repository, new CurrentUserResolver(provider));
    AuthenticatedUserResponse owner = new AuthenticatedUserResponse(10L, "owner@example.test", "Owner", 7L, 9L, null, Set.of(Role.COMPANY_OWNER));

    @BeforeEach void owner() { when(provider.currentUser()).thenReturn(Optional.of(owner)); }
    MarketplaceBranchProfile complete() {
        return MarketplaceBranchProfile.draft(7L, 9L).edit("Neiva", "Centro", "Address", "Description", "3001234567", "https://cdn.example.test/cover.jpg");
    }

    @ParameterizedTest @EnumSource(value = Role.class, names = { "COMPANY_OWNER" }, mode = EnumSource.Mode.EXCLUDE)
    void nonOwnersCannotEditAnyProfile(Role role) {
        when(provider.currentUser()).thenReturn(Optional.of(new AuthenticatedUserResponse(11L, "staff@example.test", "Staff", 7L, 9L, null, Set.of(role))));
        assertThrows(ForbiddenOperationException.class, service::currentProfile);
        assertThrows(ForbiddenOperationException.class, service::submit);
        verifyNoInteractions(repository);
    }

    @Test void updatesOnlyAuthenticatedTenantAndRevokesPublishedState() {
        var published = complete().submit().review(true, 100L, null);
        when(repository.lockOrCreate(7L, 9L)).thenReturn(published);
        var result = service.update(new UpdateMarketplaceProfileCommand("Neiva", "Norte", "New address", "Changed", "3001234567", "https://cdn.example.test/new.jpg"));
        assertEquals(MarketplacePublicationState.DRAFT, result.publicationState());
        verify(repository).save(argThat(p -> p.companyId().equals(7L) && p.branchId().equals(9L)
                && p.publicationState() == MarketplacePublicationState.DRAFT), eq(MarketplacePublicationState.PUBLISHED), eq(10L), eq(7L));
    }

    @Test void submitRequiresBookableResources() {
        when(repository.lockOrCreate(7L, 9L)).thenReturn(complete());
        assertThrows(BusinessRuleException.class, service::submit);
        verify(repository, never()).save(any(), any(), any(), any());
        when(repository.hasBookableResources(7L, 9L)).thenReturn(true);
        assertEquals(MarketplacePublicationState.PENDING_REVIEW, service.submit().publicationState());
    }

    @Test void onlyPlatformAdminReviewsAndRechecksResources() {
        assertThrows(ForbiddenOperationException.class, service::pending);
        assertThrows(ForbiddenOperationException.class, () -> service.review(9L, true, null, Instant.now()));
        when(provider.currentUser()).thenReturn(Optional.of(new AuthenticatedUserResponse(100L, "admin@example.test", "Admin", null, null, null, Set.of(Role.PLATFORM_OWNER))));
        var pending = complete().submit();
        when(repository.lockByBranch(9L)).thenReturn(Optional.of(pending));
        assertThrows(BusinessRuleException.class, () -> service.review(9L, true, null, pending.updatedAt()));
        when(repository.hasBookableResources(7L, 9L)).thenReturn(true);
        service.review(9L, true, null, pending.updatedAt());
        verify(repository).save(argThat(p -> p.publicationState() == MarketplacePublicationState.PUBLISHED),
                eq(MarketplacePublicationState.PENDING_REVIEW), eq(100L), isNull());
    }

    @Test void staleModeratorDecisionCannotApproveChangedAndResubmittedProfile() {
        when(provider.currentUser()).thenReturn(Optional.of(new AuthenticatedUserResponse(100L, "admin@example.test", "Admin", null, null, null, Set.of(Role.PLATFORM_OWNER))));
        var current = complete().submit();
        when(repository.lockByBranch(9L)).thenReturn(Optional.of(current));
        assertThrows(IdempotencyConflictException.class,
                () -> service.review(9L, true, null, current.updatedAt().minusSeconds(1)));
        assertThrows(IdempotencyConflictException.class,
                () -> service.review(9L, false, "Old snapshot rejection", current.updatedAt().minusSeconds(1)));
        verify(repository, never()).save(any(), any(), any(), any());
        verify(repository, never()).hasBookableResources(any(), any());
    }

    @Test void publicSearchDoesNotRequireIdentityAndFetchesOneExtraForNextPage() {
        var a = new PublicMarketplaceItemResponse("alpha", "main", "Alpha", "Main", "Neiva", null, "Address", "Description", "3001234567", "https://cdn.example.test/a.jpg", BigDecimal.TEN);
        when(repository.search("Neiva", null, "corte", 2, 3)).thenReturn(List.of(a, a, a));
        var page = service.search(" Neiva ", null, " corte ", 1, 2);
        assertEquals(2, page.items().size()); assertTrue(page.hasNext()); assertEquals(1, page.page());
        verifyNoInteractions(provider);
    }

    @Test void publicSearchRejectsUnboundedOrMalformedInputs() {
        assertThrows(BusinessRuleException.class, () -> service.search(null, null, null, 0, 21));
        assertThrows(BusinessRuleException.class, () -> service.search(null, null, null, -1, 12));
        assertThrows(BusinessRuleException.class, () -> service.search(null, null, null, 10001, 12));
        assertThrows(BusinessRuleException.class, () -> service.search("x".repeat(121), null, null, 0, 12));
        assertThrows(BusinessRuleException.class, () -> service.search(null, null, "bad\nquery", 0, 12));
        verifyNoInteractions(repository);
    }
}
