package com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.AppointmentJpaEntity;

public interface AppointmentJpaRepository extends JpaRepository<AppointmentJpaEntity, Long> {

	@Query("""
			select count(appointment) > 0
			from AppointmentJpaEntity appointment
			where appointment.companyId = :companyId
				and appointment.branchId = :branchId
				and appointment.barberId = :barberId
				and appointment.status in :blockingStatuses
				and appointment.startAt < :endAt
				and appointment.endAt > :startAt
			""")
	boolean existsBlockingOverlap(
			Long companyId,
			Long branchId,
			Long barberId,
			LocalDateTime startAt,
			LocalDateTime endAt,
			Collection<AppointmentStatus> blockingStatuses
	);

	Optional<AppointmentJpaEntity> findByIdAndCompanyIdAndBranchId(Long id, Long companyId, Long branchId);

	List<AppointmentJpaEntity> findAllByCompanyIdAndBranchIdAndBarberIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
			Long companyId,
			Long branchId,
			Long barberId,
			LocalDateTime startAt,
			LocalDateTime endAt
	);
}
