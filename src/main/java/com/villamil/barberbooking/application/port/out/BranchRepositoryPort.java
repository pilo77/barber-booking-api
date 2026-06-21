package com.villamil.barberbooking.application.port.out;

public interface BranchRepositoryPort {

	boolean existsByIdAndCompanyId(Long id, Long companyId);
}
