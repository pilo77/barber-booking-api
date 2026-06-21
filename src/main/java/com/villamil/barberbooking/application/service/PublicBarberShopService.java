package com.villamil.barberbooking.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.villamil.barberbooking.application.dto.response.PublicBarberResponse;
import com.villamil.barberbooking.application.dto.response.PublicBarberShopResponse;
import com.villamil.barberbooking.application.dto.response.PublicBranchResponse;
import com.villamil.barberbooking.application.dto.response.PublicServiceOfferingResponse;
import com.villamil.barberbooking.application.port.in.GetPublicBarberShopUseCase;
import com.villamil.barberbooking.application.port.in.GetPublicBranchUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicBarbersUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicBranchesUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicServicesUseCase;
import com.villamil.barberbooking.application.port.out.PublicBarberShopRepositoryPort;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.exception.PublicResourceNotFoundException;

@Service
class PublicBarberShopService implements
		GetPublicBarberShopUseCase,
		GetPublicBranchUseCase,
		ListPublicBranchesUseCase,
		ListPublicServicesUseCase,
		ListPublicBarbersUseCase {

	private final PublicBarberShopRepositoryPort publicBarberShopRepositoryPort;

	PublicBarberShopService(PublicBarberShopRepositoryPort publicBarberShopRepositoryPort) {
		this.publicBarberShopRepositoryPort = publicBarberShopRepositoryPort;
	}

	@Override
	public PublicBarberShopResponse getBySlug(String companySlug) {
		return publicBarberShopRepositoryPort.findActiveCompanyBySlug(normalizeSlug(companySlug, "Company slug"))
				.orElseThrow(() -> new PublicResourceNotFoundException("Public barber shop not found"));
	}

	@Override
	public List<PublicBranchResponse> listByCompanySlug(String companySlug) {
		String normalizedCompanySlug = normalizeSlug(companySlug, "Company slug");
		getBySlug(normalizedCompanySlug);
		return publicBarberShopRepositoryPort.findActiveBranchesByCompanySlug(normalizedCompanySlug);
	}

	@Override
	public PublicBranchResponse getBySlug(String companySlug, String branchSlug) {
		return publicBarberShopRepositoryPort.findActiveBranchBySlugs(
						normalizeSlug(companySlug, "Company slug"),
						normalizeSlug(branchSlug, "Branch slug")
				)
				.orElseThrow(() -> new PublicResourceNotFoundException("Public branch not found"));
	}

	@Override
	public List<PublicServiceOfferingResponse> listServicesByBranchSlug(String companySlug, String branchSlug) {
		String normalizedCompanySlug = normalizeSlug(companySlug, "Company slug");
		String normalizedBranchSlug = normalizeSlug(branchSlug, "Branch slug");
		getBySlug(normalizedCompanySlug, normalizedBranchSlug);
		return publicBarberShopRepositoryPort.findVisibleServicesByBranchSlugs(normalizedCompanySlug, normalizedBranchSlug);
	}

	@Override
	public List<PublicBarberResponse> listBarbersByBranchSlug(String companySlug, String branchSlug) {
		String normalizedCompanySlug = normalizeSlug(companySlug, "Company slug");
		String normalizedBranchSlug = normalizeSlug(branchSlug, "Branch slug");
		getBySlug(normalizedCompanySlug, normalizedBranchSlug);
		return publicBarberShopRepositoryPort.findVisibleBarbersByBranchSlugs(normalizedCompanySlug, normalizedBranchSlug);
	}

	private String normalizeSlug(String slug, String label) {
		if (slug == null || slug.isBlank()) {
			throw new BusinessRuleException(label + " is required");
		}
		return slug.strip().toLowerCase();
	}
}
