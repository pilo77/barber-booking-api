package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.dto.response.PublicBarberResponse;
import com.villamil.barberbooking.application.dto.response.PublicBarberShopResponse;
import com.villamil.barberbooking.application.dto.response.PublicBranchResponse;
import com.villamil.barberbooking.application.dto.response.PublicServiceOfferingResponse;
import com.villamil.barberbooking.application.dto.response.CompanyPublicBrandingResponse;
import com.villamil.barberbooking.application.port.out.PublicBarberShopRepositoryPort;
import com.villamil.barberbooking.application.tenant.PublicTenantContext;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.BarberJpaEntity;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.BranchJpaEntity;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.CompanyJpaEntity;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.CompanyPublicProfileJpaEntity;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.ServiceOfferingJpaEntity;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.BarberJpaRepository;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.BranchJpaRepository;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.CompanyPublicProfileJpaRepository;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.ServiceOfferingJpaRepository;
import com.villamil.barberbooking.domain.valueobject.ThemeMode;

@Component
public class PublicBarberShopPersistenceAdapter implements PublicBarberShopRepositoryPort {

	private final CompanyJpaRepository companyJpaRepository;
	private final CompanyPublicProfileJpaRepository companyPublicProfileJpaRepository;
	private final BranchJpaRepository branchJpaRepository;
	private final ServiceOfferingJpaRepository serviceOfferingJpaRepository;
	private final BarberJpaRepository barberJpaRepository;

	public PublicBarberShopPersistenceAdapter(
			CompanyJpaRepository companyJpaRepository,
			CompanyPublicProfileJpaRepository companyPublicProfileJpaRepository,
			BranchJpaRepository branchJpaRepository,
			ServiceOfferingJpaRepository serviceOfferingJpaRepository,
			BarberJpaRepository barberJpaRepository
	) {
		this.companyJpaRepository = companyJpaRepository;
		this.companyPublicProfileJpaRepository = companyPublicProfileJpaRepository;
		this.branchJpaRepository = branchJpaRepository;
		this.serviceOfferingJpaRepository = serviceOfferingJpaRepository;
		this.barberJpaRepository = barberJpaRepository;
	}

	@Override
	public Optional<PublicBarberShopResponse> findActiveCompanyBySlug(String companySlug) {
		return companyJpaRepository.findBySlugAndActiveTrue(companySlug)
				.map(this::toPublicCompany);
	}

	@Override
	public List<PublicBranchResponse> findActiveBranchesByCompanySlug(String companySlug) {
		return companyJpaRepository.findBySlugAndActiveTrue(companySlug)
				.map(company -> branchJpaRepository.findAllByCompanyIdAndActiveTrueOrderByIdAsc(company.getId())
						.stream()
						.map(this::toPublicBranch)
						.toList())
				.orElseGet(List::of);
	}

	@Override
	public Optional<PublicBranchResponse> findActiveBranchBySlugs(String companySlug, String branchSlug) {
		return findActiveCompanyAndBranch(companySlug, branchSlug)
				.map(this::toPublicBranch);
	}

	@Override
	public List<PublicServiceOfferingResponse> findVisibleServicesByBranchSlugs(String companySlug, String branchSlug) {
		return findActiveCompanyAndBranch(companySlug, branchSlug)
				.map(branch -> serviceOfferingJpaRepository
						.findAllByCompanyIdAndActiveTrueAndVisibleForOnlineBookingTrueOrderBySortOrderAscIdAsc(branch.getCompanyId())
						.stream()
						.map(this::toPublicService)
						.toList())
				.orElseGet(List::of);
	}

	@Override
	public List<PublicBarberResponse> findVisibleBarbersByBranchSlugs(String companySlug, String branchSlug) {
		return findActiveCompanyAndBranch(companySlug, branchSlug)
				.map(branch -> barberJpaRepository
						.findAllByCompanyIdAndBranchIdAndActiveTrueAndActiveForOnlineBookingTrueOrderBySortOrderAscIdAsc(
								branch.getCompanyId(),
								branch.getId()
						)
						.stream()
						.map(this::toPublicBarber)
						.toList())
				.orElseGet(List::of);
	}

	@Override
	public Optional<PublicTenantContext> findActiveTenantBySlugs(String companySlug, String branchSlug) {
		return findActiveCompanyAndBranch(companySlug, branchSlug)
				.map(branch -> new PublicTenantContext(branch.getCompanyId(), branch.getId()));
	}

	@Override
	public Optional<PublicServiceOfferingResponse> findVisibleServiceByCompanyId(
			Long companyId,
			Long serviceOfferingId
	) {
		return serviceOfferingJpaRepository
				.findByIdAndCompanyIdAndActiveTrueAndVisibleForOnlineBookingTrue(serviceOfferingId, companyId)
				.map(this::toPublicService);
	}

	@Override
	public Optional<PublicBarberResponse> findVisibleBarberByTenant(Long companyId, Long branchId, Long barberId) {
		return barberJpaRepository
				.findByIdAndCompanyIdAndBranchIdAndActiveTrueAndActiveForOnlineBookingTrue(
						barberId,
						companyId,
						branchId
				)
				.map(this::toPublicBarber);
	}

	@Override
	public Optional<PublicServiceOfferingResponse> findServiceSnapshotByCompanyId(
			Long companyId,
			Long serviceOfferingId
	) {
		return serviceOfferingJpaRepository.findByIdAndCompanyId(serviceOfferingId, companyId)
				.map(this::toPublicService);
	}

	@Override
	public Optional<PublicBarberResponse> findBarberSnapshotByTenant(Long companyId, Long branchId, Long barberId) {
		return barberJpaRepository.findByIdAndCompanyIdAndBranchId(barberId, companyId, branchId)
				.map(this::toPublicBarber);
	}

	private Optional<BranchJpaEntity> findActiveCompanyAndBranch(String companySlug, String branchSlug) {
		return companyJpaRepository.findBySlugAndActiveTrue(companySlug)
				.flatMap(company -> branchJpaRepository.findByCompanyIdAndSlugAndActiveTrue(company.getId(), branchSlug));
	}

	private PublicBarberShopResponse toPublicCompany(CompanyJpaEntity entity) {
		CompanyPublicBrandingResponse branding = companyPublicProfileJpaRepository.findByCompanyId(entity.getId())
				.map(profile -> toBranding(entity, profile))
				.orElseGet(() -> fallbackBranding(entity));
		return new PublicBarberShopResponse(
				entity.getSlug(),
				entity.getName(),
				entity.getDescription(),
				entity.getLogoUrl(),
				entity.isActive(),
				branding
		);
	}

	private CompanyPublicBrandingResponse toBranding(CompanyJpaEntity company, CompanyPublicProfileJpaEntity profile) {
		return new CompanyPublicBrandingResponse(
				profile.getPublicName() == null ? company.getName() : profile.getPublicName(),
				profile.getPublicDescription() == null ? company.getDescription() : profile.getPublicDescription(),
				profile.getLogoUrl() == null ? company.getLogoUrl() : profile.getLogoUrl(),
				profile.getCoverImageUrl(),
				profile.getPrimaryColor(),
				profile.getSecondaryColor(),
				profile.getAccentColor(),
				profile.getThemeMode() == null ? ThemeMode.SYSTEM : ThemeMode.valueOf(profile.getThemeMode()),
				profile.getContactPhone(),
				profile.getContactWhatsappUrl(),
				profile.getContactInstagramUrl(),
				profile.getContactFacebookUrl(),
				profile.getContactTiktokUrl(),
				profile.getContactWebsiteUrl()
		);
	}

	private CompanyPublicBrandingResponse fallbackBranding(CompanyJpaEntity entity) {
		return new CompanyPublicBrandingResponse(
				entity.getName(),
				entity.getDescription(),
				entity.getLogoUrl(),
				null,
				null,
				null,
				null,
				ThemeMode.SYSTEM,
				null,
				null,
				null,
				null,
				null,
				null
		);
	}

	private PublicBranchResponse toPublicBranch(BranchJpaEntity entity) {
		return new PublicBranchResponse(
				entity.getSlug(),
				entity.getName(),
				entity.getAddress(),
				entity.getPhone()
		);
	}

	private PublicServiceOfferingResponse toPublicService(ServiceOfferingJpaEntity entity) {
		return new PublicServiceOfferingResponse(
				entity.getId(),
				entity.getName(),
				entity.getDescription(),
				entity.getDurationMinutes(),
				entity.getPrice()
		);
	}

	private PublicBarberResponse toPublicBarber(BarberJpaEntity entity) {
		String displayName = entity.getPublicDisplayName() == null ? entity.getFullName() : entity.getPublicDisplayName();
		return new PublicBarberResponse(
				entity.getId(),
				displayName,
				entity.getPhotoUrl(),
				entity.getBio(),
				entity.getSpecialties()
		);
	}
}
