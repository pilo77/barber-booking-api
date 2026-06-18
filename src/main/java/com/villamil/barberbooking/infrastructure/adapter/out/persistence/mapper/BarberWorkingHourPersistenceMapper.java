package com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper;

import java.time.DayOfWeek;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.domain.model.BarberWorkingHour;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.BarberWorkingHourJpaEntity;

@Component
public class BarberWorkingHourPersistenceMapper {

	public BarberWorkingHourJpaEntity toEntity(BarberWorkingHour workingHour, TenantContext tenantContext) {
		return new BarberWorkingHourJpaEntity(
				workingHour.id(),
				tenantContext.companyId(),
				tenantContext.branchId(),
				workingHour.barberId(),
				(short) workingHour.dayOfWeek().getValue(),
				workingHour.startTime(),
				workingHour.endTime(),
				workingHour.active(),
				workingHour.createdAt(),
				workingHour.updatedAt()
		);
	}

	public BarberWorkingHour toDomain(BarberWorkingHourJpaEntity entity) {
		return new BarberWorkingHour(
				entity.getId(),
				entity.getBarberId(),
				DayOfWeek.of(entity.getDayOfWeek()),
				entity.getStartTime(),
				entity.getEndTime(),
				entity.isActive(),
				entity.getCreatedAt(),
				entity.getUpdatedAt()
		);
	}
}
