package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.CompanyPublicProfileRepositoryPort;
import com.villamil.barberbooking.domain.model.CompanyPublicProfile;
import com.villamil.barberbooking.domain.valueobject.ThemeMode;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.CompanyPublicProfileJpaEntity;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.CompanyPublicProfileJpaRepository;

@Component
public class CompanyPublicProfilePersistenceAdapter implements CompanyPublicProfileRepositoryPort {

	private final CompanyPublicProfileJpaRepository companyPublicProfileJpaRepository;

	public CompanyPublicProfilePersistenceAdapter(CompanyPublicProfileJpaRepository companyPublicProfileJpaRepository) {
		this.companyPublicProfileJpaRepository = companyPublicProfileJpaRepository;
	}

	@Override
	public Optional<CompanyPublicProfile> findByCompanyId(Long companyId) {
		return companyPublicProfileJpaRepository.findByCompanyId(companyId)
				.map(this::toDomain);
	}

	@Override
	public CompanyPublicProfile save(CompanyPublicProfile profile) {
		return toDomain(companyPublicProfileJpaRepository.save(toEntity(profile)));
	}

	private CompanyPublicProfile toDomain(CompanyPublicProfileJpaEntity entity) {
		return new CompanyPublicProfile(
				entity.getId(),
				entity.getCompanyId(),
				entity.getPublicName(),
				entity.getPublicDescription(),
				entity.getLogoUrl(),
				entity.getCoverImageUrl(),
				entity.getPrimaryColor(),
				entity.getSecondaryColor(),
				entity.getAccentColor(),
				ThemeMode.valueOf(entity.getThemeMode()),
				entity.getContactPhone(),
				entity.getContactWhatsappUrl(),
				entity.getContactInstagramUrl(),
				entity.getContactFacebookUrl(),
				entity.getContactTiktokUrl(),
				entity.getContactWebsiteUrl(),
				entity.getCreatedAt(),
				entity.getUpdatedAt()
		);
	}

	private CompanyPublicProfileJpaEntity toEntity(CompanyPublicProfile profile) {
		return new CompanyPublicProfileJpaEntity(
				profile.id(),
				profile.companyId(),
				profile.publicName(),
				profile.publicDescription(),
				profile.logoUrl(),
				profile.coverImageUrl(),
				profile.primaryColor(),
				profile.secondaryColor(),
				profile.accentColor(),
				profile.themeMode().name(),
				profile.contactPhone(),
				profile.whatsappUrl(),
				profile.instagramUrl(),
				profile.facebookUrl(),
				profile.tiktokUrl(),
				profile.websiteUrl(),
				profile.createdAt(),
				profile.updatedAt()
		);
	}
}
