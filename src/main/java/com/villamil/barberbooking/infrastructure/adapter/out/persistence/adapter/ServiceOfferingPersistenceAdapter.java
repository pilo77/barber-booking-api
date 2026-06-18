package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;
import com.villamil.barberbooking.domain.model.ServiceOffering;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper.ServiceOfferingPersistenceMapper;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.ServiceOfferingJpaRepository;

@Component
public class ServiceOfferingPersistenceAdapter implements ServiceOfferingRepositoryPort {

	private final ServiceOfferingJpaRepository serviceOfferingJpaRepository;
	private final ServiceOfferingPersistenceMapper serviceOfferingPersistenceMapper;

	public ServiceOfferingPersistenceAdapter(
			ServiceOfferingJpaRepository serviceOfferingJpaRepository,
			ServiceOfferingPersistenceMapper serviceOfferingPersistenceMapper
	) {
		this.serviceOfferingJpaRepository = serviceOfferingJpaRepository;
		this.serviceOfferingPersistenceMapper = serviceOfferingPersistenceMapper;
	}

	@Override
	public ServiceOffering save(ServiceOffering serviceOffering) {
		return serviceOfferingPersistenceMapper.toDomain(
				serviceOfferingJpaRepository.save(serviceOfferingPersistenceMapper.toEntity(serviceOffering))
		);
	}

	@Override
	public Optional<ServiceOffering> findById(Long id) {
		return serviceOfferingJpaRepository.findById(id)
				.map(serviceOfferingPersistenceMapper::toDomain);
	}

	@Override
	public List<ServiceOffering> findAll() {
		return serviceOfferingJpaRepository.findAll()
				.stream()
				.map(serviceOfferingPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsByName(String name) {
		return serviceOfferingJpaRepository.existsByName(name);
	}

	@Override
	public boolean existsByNameAndIdNot(String name, Long id) {
		return serviceOfferingJpaRepository.existsByNameAndIdNot(name, id);
	}
}
