package com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.CompanyJpaEntity;

public interface CompanyJpaRepository extends JpaRepository<CompanyJpaEntity, Long> {

	Optional<CompanyJpaEntity> findBySlugAndActiveTrue(String slug);
}
