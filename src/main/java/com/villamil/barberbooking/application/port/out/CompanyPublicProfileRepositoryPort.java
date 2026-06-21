package com.villamil.barberbooking.application.port.out;

import java.util.Optional;

import com.villamil.barberbooking.domain.model.CompanyPublicProfile;

public interface CompanyPublicProfileRepositoryPort {

	Optional<CompanyPublicProfile> findByCompanyId(Long companyId);

	Optional<CompanyPublicProfile> findByCompanyIdOrFallback(Long companyId);

	CompanyPublicProfile save(CompanyPublicProfile profile);
}
