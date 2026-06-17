package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.domain.model.Barber;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper.BarberPersistenceMapper;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.BarberJpaRepository;

@Component
public class BarberPersistenceAdapter implements BarberRepositoryPort {

	private final BarberJpaRepository barberJpaRepository;
	private final BarberPersistenceMapper barberPersistenceMapper;

	public BarberPersistenceAdapter(
			BarberJpaRepository barberJpaRepository,
			BarberPersistenceMapper barberPersistenceMapper
	) {
		this.barberJpaRepository = barberJpaRepository;
		this.barberPersistenceMapper = barberPersistenceMapper;
	}

	@Override
	public Barber save(Barber barber) {
		return barberPersistenceMapper.toDomain(
				barberJpaRepository.save(barberPersistenceMapper.toEntity(barber))
		);
	}

	@Override
	public Optional<Barber> findById(Long id) {
		return barberJpaRepository.findById(id)
				.map(barberPersistenceMapper::toDomain);
	}

	@Override
	public List<Barber> findAll() {
		return barberJpaRepository.findAll()
				.stream()
				.map(barberPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsByPhone(String phone) {
		return barberJpaRepository.existsByPhone(phone);
	}

	@Override
	public boolean existsByEmail(String email) {
		return barberJpaRepository.existsByEmail(email);
	}

	@Override
	public boolean existsByPhoneAndIdNot(String phone, Long id) {
		return barberJpaRepository.existsByPhoneAndIdNot(phone, id);
	}

	@Override
	public boolean existsByEmailAndIdNot(String email, Long id) {
		return barberJpaRepository.existsByEmailAndIdNot(email, id);
	}
}
