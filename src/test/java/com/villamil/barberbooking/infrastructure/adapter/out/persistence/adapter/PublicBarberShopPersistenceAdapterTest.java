package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import com.villamil.barberbooking.application.port.out.MarketplaceRepositoryPort;
import com.villamil.barberbooking.domain.model.MarketplaceBranchProfile;

import com.villamil.barberbooking.application.dto.response.PublicServiceOfferingResponse;
import com.villamil.barberbooking.domain.valueobject.ThemeMode;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.BranchJpaEntity;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.CompanyJpaEntity;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.CompanyPublicProfileJpaEntity;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.ServiceOfferingJpaEntity;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.BarberJpaRepository;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.BranchJpaRepository;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.CompanyPublicProfileJpaRepository;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.ServiceOfferingJpaRepository;

class PublicBarberShopPersistenceAdapterTest {

    private final CompanyJpaRepository companyJpaRepository = mock(CompanyJpaRepository.class);
    private final CompanyPublicProfileJpaRepository companyPublicProfileJpaRepository = mock(CompanyPublicProfileJpaRepository.class);
    private final BranchJpaRepository branchJpaRepository = mock(BranchJpaRepository.class);
    private final ServiceOfferingJpaRepository serviceOfferingJpaRepository = mock(ServiceOfferingJpaRepository.class);
    private final BarberJpaRepository barberJpaRepository = mock(BarberJpaRepository.class);
    private final MarketplaceRepositoryPort marketplace = mock(MarketplaceRepositoryPort.class);

    private final PublicBarberShopPersistenceAdapter adapter = new PublicBarberShopPersistenceAdapter(
            companyJpaRepository,
            companyPublicProfileJpaRepository,
            branchJpaRepository,
            serviceOfferingJpaRepository,
            barberJpaRepository,
            marketplace
    );

    @BeforeEach void legacyVisibility() {
        when(marketplace.isCompanyPublic(anyLong())).thenReturn(true);
        when(marketplace.isBranchPublic(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }

    @Test
    void shouldReturnCompanyVisibleServicesWhenBranchBelongsToCompany() {
        CompanyJpaEntity company = mock(CompanyJpaEntity.class);
        BranchJpaEntity branch = mock(BranchJpaEntity.class);
        ServiceOfferingJpaEntity serviceOffering = mock(ServiceOfferingJpaEntity.class);

        when(companyJpaRepository.findBySlugAndActiveTrue("ponte-perro")).thenReturn(Optional.of(company));
        when(company.getId()).thenReturn(1L);
        when(branchJpaRepository.findByCompanyIdAndSlugAndActiveTrue(1L, "neiva-centro")).thenReturn(Optional.of(branch));
        when(branch.getCompanyId()).thenReturn(1L);
        when(serviceOfferingJpaRepository.findAllByCompanyIdAndActiveTrueAndVisibleForOnlineBookingTrueOrderBySortOrderAscIdAsc(1L))
                .thenReturn(List.of(serviceOffering));

        when(serviceOffering.getId()).thenReturn(1L);
        when(serviceOffering.getName()).thenReturn("Corte clasico");
        when(serviceOffering.getDescription()).thenReturn("Corte tradicional");
        when(serviceOffering.getDurationMinutes()).thenReturn(30);
        when(serviceOffering.getPrice()).thenReturn(new BigDecimal("25000.00"));

        List<PublicServiceOfferingResponse> result = adapter.findVisibleServicesByBranchSlugs("ponte-perro", "neiva-centro");

        assertThat(result).containsExactly(new PublicServiceOfferingResponse(
                1L,
                "Corte clasico",
                "Corte tradicional",
                30,
                new BigDecimal("25000.00")
        ));
        verify(serviceOfferingJpaRepository).findAllByCompanyIdAndActiveTrueAndVisibleForOnlineBookingTrueOrderBySortOrderAscIdAsc(1L);
    }

    @Test
    void shouldFallbackBrandingWhenPublicProfileDoesNotExist() {
        CompanyJpaEntity company = mock(CompanyJpaEntity.class);

        when(companyJpaRepository.findBySlugAndActiveTrue("ponte-perro")).thenReturn(Optional.of(company));
        when(company.getId()).thenReturn(1L);
        when(company.getSlug()).thenReturn("ponte-perro");
        when(company.getName()).thenReturn("Ponte Perro");
        when(company.getDescription()).thenReturn("Cortes modernos");
        when(company.getLogoUrl()).thenReturn("https://cdn.example.com/logo.png");
        when(company.isActive()).thenReturn(true);
        when(companyPublicProfileJpaRepository.findByCompanyId(1L)).thenReturn(Optional.empty());

        var result = adapter.findActiveCompanyBySlug("ponte-perro");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().name()).isEqualTo("Ponte Perro");
        assertThat(result.orElseThrow().description()).isEqualTo("Cortes modernos");
        assertThat(result.orElseThrow().logoUrl()).isEqualTo("https://cdn.example.com/logo.png");
        assertThat(result.orElseThrow().branding().publicName()).isEqualTo("Ponte Perro");
        assertThat(result.orElseThrow().branding().themeMode()).isEqualTo(ThemeMode.SYSTEM);
    }

    @Test
    void shouldUseStoredBrandingWhenPublicProfileExists() {
        CompanyJpaEntity company = mock(CompanyJpaEntity.class);
        CompanyPublicProfileJpaEntity profile = mock(CompanyPublicProfileJpaEntity.class);

        when(companyJpaRepository.findBySlugAndActiveTrue("ponte-perro")).thenReturn(Optional.of(company));
        when(company.getId()).thenReturn(1L);
        when(company.getSlug()).thenReturn("ponte-perro");
        when(company.getName()).thenReturn("Ponte Perro");
        when(company.getDescription()).thenReturn("Cortes modernos");
        when(company.getLogoUrl()).thenReturn("https://cdn.example.com/logo-old.png");
        when(company.isActive()).thenReturn(true);
        when(companyPublicProfileJpaRepository.findByCompanyId(1L)).thenReturn(Optional.of(profile));
        when(profile.getPublicName()).thenReturn("Ponte Perro Premium");
        when(profile.getPublicDescription()).thenReturn("Branding nuevo");
        when(profile.getLogoUrl()).thenReturn("https://cdn.example.com/logo-new.png");
        when(profile.getPrimaryColor()).thenReturn("#111111");
        when(profile.getAccentColor()).thenReturn("#D4AF37");
        when(profile.getThemeMode()).thenReturn("DARK");

        var result = adapter.findActiveCompanyBySlug("ponte-perro");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().name()).isEqualTo("Ponte Perro Premium");
        assertThat(result.orElseThrow().description()).isEqualTo("Branding nuevo");
        assertThat(result.orElseThrow().logoUrl()).isEqualTo("https://cdn.example.com/logo-new.png");
        assertThat(result.orElseThrow().branding().publicName()).isEqualTo("Ponte Perro Premium");
        assertThat(result.orElseThrow().branding().logoUrl()).isEqualTo("https://cdn.example.com/logo-new.png");
        assertThat(result.orElseThrow().branding().themeMode()).isEqualTo(ThemeMode.DARK);
    }

    @Test
    void shouldApplyFieldByFieldFallbackWhenStoredBrandingHasNullValues() {
        CompanyJpaEntity company = mock(CompanyJpaEntity.class);
        CompanyPublicProfileJpaEntity profile = mock(CompanyPublicProfileJpaEntity.class);

        when(companyJpaRepository.findBySlugAndActiveTrue("ponte-perro")).thenReturn(Optional.of(company));
        when(company.getId()).thenReturn(1L);
        when(company.getSlug()).thenReturn("ponte-perro");
        when(company.getName()).thenReturn("Ponte Perro");
        when(company.getDescription()).thenReturn("Descripcion base");
        when(company.getLogoUrl()).thenReturn("https://cdn.example.com/base-logo.png");
        when(company.isActive()).thenReturn(true);
        when(companyPublicProfileJpaRepository.findByCompanyId(1L)).thenReturn(Optional.of(profile));
        when(profile.getPublicName()).thenReturn(null);
        when(profile.getPublicDescription()).thenReturn("Descripcion nueva");
        when(profile.getLogoUrl()).thenReturn(null);
        when(profile.getThemeMode()).thenReturn(null);

        var result = adapter.findActiveCompanyBySlug("ponte-perro");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().name()).isEqualTo("Ponte Perro");
        assertThat(result.orElseThrow().description()).isEqualTo("Descripcion nueva");
        assertThat(result.orElseThrow().logoUrl()).isEqualTo("https://cdn.example.com/base-logo.png");
        assertThat(result.orElseThrow().branding().publicName()).isEqualTo("Ponte Perro");
        assertThat(result.orElseThrow().branding().logoUrl()).isEqualTo("https://cdn.example.com/base-logo.png");
        assertThat(result.orElseThrow().branding().themeMode()).isEqualTo(ThemeMode.SYSTEM);
    }

    @Test
    void shouldNotQueryServicesWhenBranchDoesNotBelongToCompany() {
        CompanyJpaEntity company = mock(CompanyJpaEntity.class);

        when(companyJpaRepository.findBySlugAndActiveTrue("ponte-perro")).thenReturn(Optional.of(company));
        when(branchJpaRepository.findByCompanyIdAndSlugAndActiveTrue(1L, "other-branch")).thenReturn(Optional.empty());
        when(company.getId()).thenReturn(1L);

        List<PublicServiceOfferingResponse> result = adapter.findVisibleServicesByBranchSlugs("ponte-perro", "other-branch");

        assertThat(result).isEmpty();
        verify(serviceOfferingJpaRepository, never())
                .findAllByCompanyIdAndActiveTrueAndVisibleForOnlineBookingTrueOrderBySortOrderAscIdAsc(anyLong());
    }

    @Test void explicitPrivatePublicationHidesCompanyAndAllDirectResourceMethods() {
        CompanyJpaEntity company = mock(CompanyJpaEntity.class);
        when(companyJpaRepository.findBySlugAndActiveTrue("private-shop")).thenReturn(Optional.of(company));
        when(company.getId()).thenReturn(7L);
        when(marketplace.isCompanyPublic(7L)).thenReturn(false);
        when(marketplace.isBranchPublic(7L, 9L)).thenReturn(false);
        assertThat(adapter.findActiveCompanyBySlug("private-shop")).isEmpty();
        assertThat(adapter.findVisibleServiceByCompanyId(7L, 1L)).isEmpty();
        assertThat(adapter.findServiceSnapshotByCompanyId(7L, 1L)).isEmpty();
        assertThat(adapter.findVisibleBarberByTenant(7L, 9L, 1L)).isEmpty();
        assertThat(adapter.findBarberSnapshotByTenant(7L, 9L, 1L)).isEmpty();
        verify(serviceOfferingJpaRepository, never()).findByIdAndCompanyId(1L, 7L);
        verify(barberJpaRepository, never()).findByIdAndCompanyIdAndBranchId(1L, 7L, 9L);
    }

    @Test void explicitPrivateBranchCannotResolvePublicTenantOrLists() {
        CompanyJpaEntity company = mock(CompanyJpaEntity.class);
        BranchJpaEntity branch = mock(BranchJpaEntity.class);
        when(companyJpaRepository.findBySlugAndActiveTrue("private-shop")).thenReturn(Optional.of(company));
        when(company.getId()).thenReturn(7L);
        when(branch.getId()).thenReturn(9L); when(branch.getCompanyId()).thenReturn(7L);
        when(branchJpaRepository.findByCompanyIdAndSlugAndActiveTrue(7L, "main")).thenReturn(Optional.of(branch));
        when(branchJpaRepository.findAllByCompanyIdAndActiveTrueOrderByIdAsc(7L)).thenReturn(List.of(branch));
        when(marketplace.isBranchPublic(7L, 9L)).thenReturn(false);
        assertThat(adapter.findActiveBranchesByCompanySlug("private-shop")).isEmpty();
        assertThat(adapter.findActiveBranchBySlugs("private-shop", "main")).isEmpty();
        assertThat(adapter.findActiveTenantBySlugs("private-shop", "main")).isEmpty();
        assertThat(adapter.findVisibleServicesByBranchSlugs("private-shop", "main")).isEmpty();
        assertThat(adapter.findVisibleBarbersByBranchSlugs("private-shop", "main")).isEmpty();
    }

    @Test void moderatedMarketplaceContentOverridesUnreviewedLegacyBranding() {
        CompanyJpaEntity company = mock(CompanyJpaEntity.class);
        CompanyPublicProfileJpaEntity legacy = mock(CompanyPublicProfileJpaEntity.class);
        when(companyJpaRepository.findBySlugAndActiveTrue("reviewed-shop")).thenReturn(Optional.of(company));
        when(company.getId()).thenReturn(7L); when(company.getName()).thenReturn("Reviewed shop");
        when(companyPublicProfileJpaRepository.findByCompanyId(7L)).thenReturn(Optional.of(legacy));
        when(legacy.getPublicDescription()).thenReturn("Unreviewed draft copy");
        var published = MarketplaceBranchProfile.draft(7L, 9L).edit("Neiva", "Centro", "Reviewed address",
                "Reviewed description", "3001234567", "https://cdn.example.test/reviewed.jpg").submit().review(true, 100L, null);
        when(marketplace.findPublishedCompanyProfile(7L)).thenReturn(Optional.of(published));
        var result = adapter.findActiveCompanyBySlug("reviewed-shop").orElseThrow();
        assertThat(result.name()).isEqualTo("Reviewed shop");
        assertThat(result.description()).isEqualTo("Reviewed description");
        assertThat(result.branding().coverImageUrl()).isEqualTo("https://cdn.example.test/reviewed.jpg");
        assertThat(result.branding().contactPhone()).isEqualTo("3001234567");
    }
}
