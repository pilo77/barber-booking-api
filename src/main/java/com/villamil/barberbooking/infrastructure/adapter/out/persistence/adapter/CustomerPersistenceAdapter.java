package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.TenantContextProvider;
import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.domain.model.Customer;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper.CustomerPersistenceMapper;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.CustomerJpaRepository;

@Component
public class CustomerPersistenceAdapter implements CustomerRepositoryPort {

	private final CustomerJpaRepository customerJpaRepository;
	private final CustomerPersistenceMapper customerPersistenceMapper;
	private final TenantContextProvider tenantContextProvider;

	public CustomerPersistenceAdapter(
			CustomerJpaRepository customerJpaRepository,
			CustomerPersistenceMapper customerPersistenceMapper,
			TenantContextProvider tenantContextProvider
	) {
		this.customerJpaRepository = customerJpaRepository;
		this.customerPersistenceMapper = customerPersistenceMapper;
		this.tenantContextProvider = tenantContextProvider;
	}

	@Override
	public Customer save(Customer customer) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return customerPersistenceMapper.toDomain(
				customerJpaRepository.save(customerPersistenceMapper.toEntity(customer, tenantContext))
		);
	}

	@Override
	public Optional<Customer> findById(Long id) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return customerJpaRepository.findByIdAndCompanyId(id, tenantContext.companyId())
				.map(customerPersistenceMapper::toDomain);
	}

	@Override
	public List<Customer> findAll() {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return customerJpaRepository.findAllByCompanyIdOrderByIdAsc(tenantContext.companyId())
				.stream()
				.map(customerPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsByPhone(String phone) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return customerJpaRepository.existsByCompanyIdAndPhone(tenantContext.companyId(), phone);
	}

	@Override
	public boolean existsByEmail(String email) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return customerJpaRepository.existsByCompanyIdAndEmail(tenantContext.companyId(), email);
	}

	@Override
	public boolean existsByPhoneAndIdNot(String phone, Long id) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return customerJpaRepository.existsByCompanyIdAndPhoneAndIdNot(tenantContext.companyId(), phone, id);
	}

	@Override
	public boolean existsByEmailAndIdNot(String email, Long id) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return customerJpaRepository.existsByCompanyIdAndEmailAndIdNot(tenantContext.companyId(), email, id);
	}
}
