package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.application.port.out.TenantContextProvider;
import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.domain.model.Barber;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper.BarberPersistenceMapper;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.BarberJpaRepository;

@Component
public class BarberPersistenceAdapter implements BarberRepositoryPort {

	private final BarberJpaRepository barberJpaRepository;
	private final BarberPersistenceMapper barberPersistenceMapper;
	private final TenantContextProvider tenantContextProvider;

	public BarberPersistenceAdapter(
			BarberJpaRepository barberJpaRepository,
			BarberPersistenceMapper barberPersistenceMapper,
			TenantContextProvider tenantContextProvider
	) {
		this.barberJpaRepository = barberJpaRepository;
		this.barberPersistenceMapper = barberPersistenceMapper;
		this.tenantContextProvider = tenantContextProvider;
	}

	@Override
	public Barber save(Barber barber) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return barberPersistenceMapper.toDomain(
				barberJpaRepository.save(barberPersistenceMapper.toEntity(barber, tenantContext))
		);
	}

	@Override
	public Optional<Barber> findById(Long id) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return barberJpaRepository.findByIdAndCompanyIdAndBranchId(
				id,
				tenantContext.companyId(),
				tenantContext.branchId()
		)
				.map(barberPersistenceMapper::toDomain);
	}

	@Override
	public List<Barber> findAll() {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return barberJpaRepository.findAllByCompanyIdAndBranchIdOrderByIdAsc(
				tenantContext.companyId(),
				tenantContext.branchId()
		)
				.stream()
				.map(barberPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsByPhone(String phone) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return barberJpaRepository.existsByCompanyIdAndPhone(tenantContext.companyId(), phone);
	}

	@Override
	public boolean existsByEmail(String email) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return barberJpaRepository.existsByCompanyIdAndEmail(tenantContext.companyId(), email);
	}

	@Override
	public boolean existsByPhoneAndIdNot(String phone, Long id) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return barberJpaRepository.existsByCompanyIdAndPhoneAndIdNot(tenantContext.companyId(), phone, id);
	}

	@Override
	public boolean existsByEmailAndIdNot(String email, Long id) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return barberJpaRepository.existsByCompanyIdAndEmailAndIdNot(tenantContext.companyId(), email, id);
	}
}
