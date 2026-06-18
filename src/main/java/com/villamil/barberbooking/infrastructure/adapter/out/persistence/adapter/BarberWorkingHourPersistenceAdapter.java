package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;
import com.villamil.barberbooking.domain.model.BarberWorkingHour;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper.BarberWorkingHourPersistenceMapper;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.BarberWorkingHourJpaRepository;

@Component
public class BarberWorkingHourPersistenceAdapter implements BarberWorkingHourRepositoryPort {

	private final BarberWorkingHourJpaRepository barberWorkingHourJpaRepository;
	private final BarberWorkingHourPersistenceMapper barberWorkingHourPersistenceMapper;

	public BarberWorkingHourPersistenceAdapter(
			BarberWorkingHourJpaRepository barberWorkingHourJpaRepository,
			BarberWorkingHourPersistenceMapper barberWorkingHourPersistenceMapper
	) {
		this.barberWorkingHourJpaRepository = barberWorkingHourJpaRepository;
		this.barberWorkingHourPersistenceMapper = barberWorkingHourPersistenceMapper;
	}

	@Override
	public BarberWorkingHour save(BarberWorkingHour workingHour) {
		return barberWorkingHourPersistenceMapper.toDomain(
				barberWorkingHourJpaRepository.save(barberWorkingHourPersistenceMapper.toEntity(workingHour))
		);
	}

	@Override
	public Optional<BarberWorkingHour> findByIdAndBarberId(Long id, Long barberId) {
		return barberWorkingHourJpaRepository.findByIdAndBarberId(id, barberId)
				.map(barberWorkingHourPersistenceMapper::toDomain);
	}

	@Override
	public List<BarberWorkingHour> findAllByBarberId(Long barberId) {
		return barberWorkingHourJpaRepository.findAllByBarberIdOrderByDayOfWeekAscStartTimeAsc(barberId)
				.stream()
				.map(barberWorkingHourPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<BarberWorkingHour> findActiveByBarberIdAndDay(Long barberId, DayOfWeek dayOfWeek) {
		return barberWorkingHourJpaRepository
				.findAllByBarberIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
						barberId,
						(short) dayOfWeek.getValue()
				)
				.stream()
				.map(barberWorkingHourPersistenceMapper::toDomain)
				.toList();
	}
}
