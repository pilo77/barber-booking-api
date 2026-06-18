package com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.BarberWorkingHourJpaEntity;

public interface BarberWorkingHourJpaRepository extends JpaRepository<BarberWorkingHourJpaEntity, Long> {

	Optional<BarberWorkingHourJpaEntity> findByIdAndBarberId(Long id, Long barberId);

	List<BarberWorkingHourJpaEntity> findAllByBarberIdOrderByDayOfWeekAscStartTimeAsc(Long barberId);

	List<BarberWorkingHourJpaEntity> findAllByBarberIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
			Long barberId,
			short dayOfWeek
	);
}
