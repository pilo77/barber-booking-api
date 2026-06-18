package com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.AppointmentJpaEntity;

public interface AppointmentJpaRepository extends JpaRepository<AppointmentJpaEntity, Long> {

	@Query("""
			select count(appointment) > 0
			from AppointmentJpaEntity appointment
			where appointment.barberId = :barberId
				and appointment.status in :blockingStatuses
				and appointment.startAt < :endAt
				and appointment.endAt > :startAt
			""")
	boolean existsBlockingOverlap(
			Long barberId,
			LocalDateTime startAt,
			LocalDateTime endAt,
			Collection<AppointmentStatus> blockingStatuses
	);

	List<AppointmentJpaEntity> findAllByBarberIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
			Long barberId,
			LocalDateTime startAt,
			LocalDateTime endAt
	);
}
