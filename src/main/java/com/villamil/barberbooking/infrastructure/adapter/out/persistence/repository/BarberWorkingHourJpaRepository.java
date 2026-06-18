package com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.BarberWorkingHourJpaEntity;

public interface BarberWorkingHourJpaRepository extends JpaRepository<BarberWorkingHourJpaEntity, Long> {

	Optional<BarberWorkingHourJpaEntity> findByIdAndBarberIdAndCompanyIdAndBranchId(
			Long id,
			Long barberId,
			Long companyId,
			Long branchId
	);

	List<BarberWorkingHourJpaEntity> findAllByBarberIdAndCompanyIdAndBranchIdOrderByDayOfWeekAscStartTimeAsc(
			Long barberId,
			Long companyId,
			Long branchId
	);

	List<BarberWorkingHourJpaEntity> findAllByBarberIdAndCompanyIdAndBranchIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
			Long barberId,
			Long companyId,
			Long branchId,
			short dayOfWeek
	);
}
