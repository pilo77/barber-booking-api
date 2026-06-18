package com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.BranchJpaEntity;

public interface BranchJpaRepository extends JpaRepository<BranchJpaEntity, Long> {
}
