package com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.domain.model.ServiceOffering;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.ServiceOfferingJpaEntity;

@Component
public class ServiceOfferingPersistenceMapper {

	public ServiceOfferingJpaEntity toEntity(ServiceOffering serviceOffering, TenantContext tenantContext) {
		return new ServiceOfferingJpaEntity(
				serviceOffering.id(),
				tenantContext.companyId(),
				serviceOffering.name(),
				serviceOffering.description(),
				serviceOffering.durationMinutes(),
				serviceOffering.price(),
				serviceOffering.active(),
				serviceOffering.createdAt(),
				serviceOffering.updatedAt()
		);
	}

	public ServiceOffering toDomain(ServiceOfferingJpaEntity entity) {
		return new ServiceOffering(
				entity.getId(),
				entity.getName(),
				entity.getDescription(),
				entity.getDurationMinutes(),
				entity.getPrice(),
				entity.isActive(),
				entity.getCreatedAt(),
				entity.getUpdatedAt()
		);
	}
}
