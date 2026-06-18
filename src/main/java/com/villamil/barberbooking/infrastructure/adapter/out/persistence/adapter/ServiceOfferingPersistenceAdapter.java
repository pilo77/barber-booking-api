package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;
import com.villamil.barberbooking.application.port.out.TenantContextProvider;
import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.domain.model.ServiceOffering;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper.ServiceOfferingPersistenceMapper;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.ServiceOfferingJpaRepository;

@Component
public class ServiceOfferingPersistenceAdapter implements ServiceOfferingRepositoryPort {

	private final ServiceOfferingJpaRepository serviceOfferingJpaRepository;
	private final ServiceOfferingPersistenceMapper serviceOfferingPersistenceMapper;
	private final TenantContextProvider tenantContextProvider;

	public ServiceOfferingPersistenceAdapter(
			ServiceOfferingJpaRepository serviceOfferingJpaRepository,
			ServiceOfferingPersistenceMapper serviceOfferingPersistenceMapper,
			TenantContextProvider tenantContextProvider
	) {
		this.serviceOfferingJpaRepository = serviceOfferingJpaRepository;
		this.serviceOfferingPersistenceMapper = serviceOfferingPersistenceMapper;
		this.tenantContextProvider = tenantContextProvider;
	}

	@Override
	public ServiceOffering save(ServiceOffering serviceOffering) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return serviceOfferingPersistenceMapper.toDomain(
				serviceOfferingJpaRepository.save(serviceOfferingPersistenceMapper.toEntity(serviceOffering, tenantContext))
		);
	}

	@Override
	public Optional<ServiceOffering> findById(Long id) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return serviceOfferingJpaRepository.findByIdAndCompanyId(id, tenantContext.companyId())
				.map(serviceOfferingPersistenceMapper::toDomain);
	}

	@Override
	public List<ServiceOffering> findAll() {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return serviceOfferingJpaRepository.findAllByCompanyIdOrderByIdAsc(tenantContext.companyId())
				.stream()
				.map(serviceOfferingPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsByName(String name) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return serviceOfferingJpaRepository.existsByCompanyIdAndName(tenantContext.companyId(), name);
	}

	@Override
	public boolean existsByNameAndIdNot(String name, Long id) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return serviceOfferingJpaRepository.existsByCompanyIdAndNameAndIdNot(tenantContext.companyId(), name, id);
	}
}
