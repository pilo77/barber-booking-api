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

import com.villamil.barberbooking.application.dto.response.PublicServiceOfferingResponse;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.BranchJpaEntity;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.CompanyJpaEntity;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.ServiceOfferingJpaEntity;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.BarberJpaRepository;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.BranchJpaRepository;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.ServiceOfferingJpaRepository;

class PublicBarberShopPersistenceAdapterTest {

    private final CompanyJpaRepository companyJpaRepository = mock(CompanyJpaRepository.class);
    private final BranchJpaRepository branchJpaRepository = mock(BranchJpaRepository.class);
    private final ServiceOfferingJpaRepository serviceOfferingJpaRepository = mock(ServiceOfferingJpaRepository.class);
    private final BarberJpaRepository barberJpaRepository = mock(BarberJpaRepository.class);

    private final PublicBarberShopPersistenceAdapter adapter = new PublicBarberShopPersistenceAdapter(
            companyJpaRepository,
            branchJpaRepository,
            serviceOfferingJpaRepository,
            barberJpaRepository
    );

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
}
