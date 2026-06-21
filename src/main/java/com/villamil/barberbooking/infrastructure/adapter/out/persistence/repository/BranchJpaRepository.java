package com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.BranchJpaEntity;

public interface BranchJpaRepository extends JpaRepository<BranchJpaEntity, Long> {

	boolean existsByIdAndCompanyId(Long id, Long companyId);

	List<BranchJpaEntity> findAllByCompanyIdAndActiveTrueOrderByIdAsc(Long companyId);

	Optional<BranchJpaEntity> findByCompanyIdAndSlugAndActiveTrue(Long companyId, String slug);
}
