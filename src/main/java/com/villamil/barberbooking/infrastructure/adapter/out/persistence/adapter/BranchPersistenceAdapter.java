package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.BranchRepositoryPort;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.BranchJpaRepository;

@Component
public class BranchPersistenceAdapter implements BranchRepositoryPort {

	private final BranchJpaRepository branchJpaRepository;

	public BranchPersistenceAdapter(BranchJpaRepository branchJpaRepository) {
		this.branchJpaRepository = branchJpaRepository;
	}

	@Override
	public boolean existsByIdAndCompanyId(Long id, Long companyId) {
		return branchJpaRepository.existsByIdAndCompanyId(id, companyId);
	}
}
