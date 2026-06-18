package com.villamil.barberbooking.application.port.out;

import java.util.List;
import java.util.Optional;

import com.villamil.barberbooking.application.dto.response.PublicBarberResponse;
import com.villamil.barberbooking.application.dto.response.PublicBarberShopResponse;
import com.villamil.barberbooking.application.dto.response.PublicBranchResponse;
import com.villamil.barberbooking.application.dto.response.PublicServiceOfferingResponse;
import com.villamil.barberbooking.application.tenant.PublicTenantContext;

public interface PublicBarberShopRepositoryPort {

	Optional<PublicBarberShopResponse> findActiveCompanyBySlug(String companySlug);

	List<PublicBranchResponse> findActiveBranchesByCompanySlug(String companySlug);

	Optional<PublicBranchResponse> findActiveBranchBySlugs(String companySlug, String branchSlug);

	List<PublicServiceOfferingResponse> findVisibleServicesByBranchSlugs(String companySlug, String branchSlug);

	List<PublicBarberResponse> findVisibleBarbersByBranchSlugs(String companySlug, String branchSlug);

	Optional<PublicTenantContext> findActiveTenantBySlugs(String companySlug, String branchSlug);

	Optional<PublicServiceOfferingResponse> findVisibleServiceByCompanyId(Long companyId, Long serviceOfferingId);

	Optional<PublicBarberResponse> findVisibleBarberByTenant(Long companyId, Long branchId, Long barberId);
}
