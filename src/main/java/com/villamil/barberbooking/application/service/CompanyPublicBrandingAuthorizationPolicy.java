package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;
import com.villamil.barberbooking.domain.model.Role;

@Component
class CompanyPublicBrandingAuthorizationPolicy {

	void ensureCanManageCompanyBranding(AuthenticatedUserResponse actor) {
		if (actor.roles().contains(Role.PLATFORM_OWNER) || actor.roles().contains(Role.COMPANY_OWNER)) {
			return;
		}
		throw new ForbiddenOperationException("User role cannot manage company public branding");
	}
}
