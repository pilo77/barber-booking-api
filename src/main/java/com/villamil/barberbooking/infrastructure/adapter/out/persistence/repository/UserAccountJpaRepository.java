package com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.UserAccountJpaEntity;

public interface UserAccountJpaRepository extends JpaRepository<UserAccountJpaEntity, Long> {

	boolean existsByEmail(String email);

	boolean existsByBarberId(Long barberId);

	Optional<UserAccountJpaEntity> findByEmail(String email);

	List<UserAccountJpaEntity> findAllByCompanyIdOrderByIdAsc(Long companyId);

	List<UserAccountJpaEntity> findAllByCompanyIdAndBranchIdOrderByIdAsc(Long companyId, Long branchId);
}
