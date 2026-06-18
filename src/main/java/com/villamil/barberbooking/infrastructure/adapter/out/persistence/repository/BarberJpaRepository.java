package com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.BarberJpaEntity;

public interface BarberJpaRepository extends JpaRepository<BarberJpaEntity, Long> {

	Optional<BarberJpaEntity> findByIdAndCompanyIdAndBranchId(Long id, Long companyId, Long branchId);

	List<BarberJpaEntity> findAllByCompanyIdAndBranchIdOrderByIdAsc(Long companyId, Long branchId);

	boolean existsByCompanyIdAndPhone(Long companyId, String phone);

	boolean existsByCompanyIdAndEmail(Long companyId, String email);

	boolean existsByCompanyIdAndPhoneAndIdNot(Long companyId, String phone, Long id);

	boolean existsByCompanyIdAndEmailAndIdNot(Long companyId, String email, Long id);
}
