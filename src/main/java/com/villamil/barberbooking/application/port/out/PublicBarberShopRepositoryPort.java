package com.villamil.barberbooking.application.port.out;

import java.util.List;
import java.util.Optional;

import com.villamil.barberbooking.application.dto.response.PublicBarberResponse;
import com.villamil.barberbooking.application.dto.response.PublicBarberShopResponse;
import com.villamil.barberbooking.application.dto.response.PublicBranchResponse;
import com.villamil.barberbooking.application.dto.response.PublicServiceOfferingResponse;

public interface PublicBarberShopRepositoryPort {

	Optional<PublicBarberShopResponse> findActiveCompanyBySlug(String companySlug);

	List<PublicBranchResponse> findActiveBranchesByCompanySlug(String companySlug);

	Optional<PublicBranchResponse> findActiveBranchBySlugs(String companySlug, String branchSlug);

	List<PublicServiceOfferingResponse> findVisibleServicesByBranchSlugs(String companySlug, String branchSlug);

	List<PublicBarberResponse> findVisibleBarbersByBranchSlugs(String companySlug, String branchSlug);
}
