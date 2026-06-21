package com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.CustomerJpaEntity;

public interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, Long> {

	Optional<CustomerJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

	Optional<CustomerJpaEntity> findByCompanyIdAndPhone(Long companyId, String phone);

	List<CustomerJpaEntity> findAllByCompanyIdOrderByIdAsc(Long companyId);

	boolean existsByCompanyIdAndPhone(Long companyId, String phone);

	boolean existsByCompanyIdAndEmail(Long companyId, String email);

	boolean existsByCompanyIdAndPhoneAndIdNot(Long companyId, String phone, Long id);

	boolean existsByCompanyIdAndEmailAndIdNot(Long companyId, String email, Long id);
}
