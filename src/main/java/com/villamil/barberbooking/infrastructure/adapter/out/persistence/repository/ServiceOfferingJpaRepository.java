package com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.ServiceOfferingJpaEntity;

public interface ServiceOfferingJpaRepository extends JpaRepository<ServiceOfferingJpaEntity, Long> {

	Optional<ServiceOfferingJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

	List<ServiceOfferingJpaEntity> findAllByCompanyIdOrderByIdAsc(Long companyId);

	List<ServiceOfferingJpaEntity> findAllByCompanyIdAndActiveTrueAndVisibleForOnlineBookingTrueOrderBySortOrderAscIdAsc(Long companyId);

	boolean existsByCompanyIdAndName(Long companyId, String name);

	boolean existsByCompanyIdAndNameAndIdNot(Long companyId, String name, Long id);
}
