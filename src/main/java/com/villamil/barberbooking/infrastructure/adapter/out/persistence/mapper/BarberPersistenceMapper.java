package com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.domain.model.Barber;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.BarberJpaEntity;

@Component
public class BarberPersistenceMapper {

	public BarberJpaEntity toEntity(Barber barber, TenantContext tenantContext) {
		return new BarberJpaEntity(
				barber.id(),
				tenantContext.companyId(),
				tenantContext.branchId(),
				barber.fullName(),
				barber.phone(),
				barber.email(),
				barber.active(),
				barber.createdAt(),
				barber.updatedAt()
		);
	}

	public Barber toDomain(BarberJpaEntity entity) {
		return new Barber(
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
