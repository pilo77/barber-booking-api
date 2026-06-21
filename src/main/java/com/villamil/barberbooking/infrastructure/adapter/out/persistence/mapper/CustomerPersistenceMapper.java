package com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.domain.model.Customer;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.CustomerJpaEntity;

@Component
public class CustomerPersistenceMapper {

	public CustomerJpaEntity toEntity(Customer customer, TenantContext tenantContext) {
		return new CustomerJpaEntity(
				customer.id(),
				tenantContext.companyId(),
				customer.fullName(),
				customer.phone(),
				customer.email(),
				customer.active(),
				customer.createdAt(),
				customer.updatedAt()
		);
	}

	public Customer toDomain(CustomerJpaEntity entity) {
		return new Customer(
				entity.getId(),
				entity.getFullName(),
				entity.getPhone(),
				entity.getEmail(),
				entity.isActive(),
				entity.getCreatedAt(),
				entity.getUpdatedAt()
		);
	}
}
