package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.UpdateCompanyPublicBrandingCommand;
import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.dto.response.CompanyPublicBrandingResponse;
import com.villamil.barberbooking.application.port.in.GetCurrentCompanyPublicBrandingUseCase;
import com.villamil.barberbooking.application.port.in.UpdateCompanyPublicBrandingUseCase;
import com.villamil.barberbooking.application.port.out.CompanyPublicProfileRepositoryPort;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.model.CompanyPublicProfile;

@Service
class CompanyPublicBrandingService implements
		GetCurrentCompanyPublicBrandingUseCase,
		UpdateCompanyPublicBrandingUseCase {

	private final CompanyPublicProfileRepositoryPort companyPublicProfileRepositoryPort;
	private final CurrentUserResolver currentUserResolver;
	private final CompanyPublicBrandingAuthorizationPolicy authorizationPolicy;
	private final CompanyPublicBrandingValidator companyPublicBrandingValidator;

	CompanyPublicBrandingService(
			CompanyPublicProfileRepositoryPort companyPublicProfileRepositoryPort,
			CurrentUserResolver currentUserResolver,
			CompanyPublicBrandingAuthorizationPolicy authorizationPolicy,
			CompanyPublicBrandingValidator companyPublicBrandingValidator
	) {
		this.companyPublicProfileRepositoryPort = companyPublicProfileRepositoryPort;
		this.currentUserResolver = currentUserResolver;
		this.authorizationPolicy = authorizationPolicy;
		this.companyPublicBrandingValidator = companyPublicBrandingValidator;
	}

	@Override
	@Transactional(readOnly = true)
	public CompanyPublicBrandingResponse getCurrent() {
		AuthenticatedUserResponse actor = requireAuthorizedActor();
		return CompanyPublicBrandingResponse.from(loadProfile(actor.companyId()));
	}

	@Override
	@Transactional
	public CompanyPublicBrandingResponse update(UpdateCompanyPublicBrandingCommand command) {
		AuthenticatedUserResponse actor = requireAuthorizedActor();
		CompanyPublicProfile currentProfile = loadProfile(actor.companyId());
		CompanyPublicBrandingValidator.NormalizedCompanyPublicBranding normalized = companyPublicBrandingValidator.normalize(command);
		CompanyPublicProfile updatedProfile = currentProfile.update(
				normalized.publicName(),
				normalized.publicDescription(),
				normalized.logoUrl(),
				normalized.coverImageUrl(),
				normalized.primaryColor(),
				normalized.secondaryColor(),
				normalized.accentColor(),
				normalized.themeMode(),
				normalized.contactPhone(),
				normalized.whatsappUrl(),
				normalized.instagramUrl(),
				normalized.facebookUrl(),
				normalized.tiktokUrl(),
				normalized.websiteUrl()
		);
		return CompanyPublicBrandingResponse.from(companyPublicProfileRepositoryPort.save(updatedProfile));
	}

	private AuthenticatedUserResponse requireAuthorizedActor() {
		AuthenticatedUserResponse actor = currentUserResolver.requireCurrentUser();
		authorizationPolicy.ensureCanManageCompanyBranding(actor);
		if (actor.companyId() == null || actor.companyId() <= 0) {
			throw new BusinessRuleException("Authenticated user requires a company scope");
		}
		return actor;
	}

	private CompanyPublicProfile loadProfile(Long companyId) {
		return companyPublicProfileRepositoryPort.findByCompanyIdOrFallback(companyId)
				.orElseThrow(() -> new BusinessRuleException("Company public profile not found"));
	}
}
