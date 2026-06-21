package com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.CompanyPublicProfileJpaEntity;

public interface CompanyPublicProfileJpaRepository extends JpaRepository<CompanyPublicProfileJpaEntity, Long> {

	Optional<CompanyPublicProfileJpaEntity> findByCompanyId(Long companyId);
}
