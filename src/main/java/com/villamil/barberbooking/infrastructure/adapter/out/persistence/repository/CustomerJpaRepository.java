package com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.CustomerJpaEntity;

public interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, Long> {

	boolean existsByPhone(String phone);

	boolean existsByEmail(String email);

	boolean existsByPhoneAndIdNot(String phone, Long id);

	boolean existsByEmailAndIdNot(String email, Long id);
}
