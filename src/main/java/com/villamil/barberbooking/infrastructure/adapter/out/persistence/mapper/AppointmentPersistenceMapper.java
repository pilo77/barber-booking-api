package com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.domain.model.Appointment;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.AppointmentJpaEntity;

@Component
public class AppointmentPersistenceMapper {

	public AppointmentJpaEntity toEntity(Appointment appointment) {
		return new AppointmentJpaEntity(
				appointment.id(),
				appointment.customerId(),
				appointment.barberId(),
				appointment.serviceOfferingId(),
				appointment.startAt(),
				appointment.endAt(),
				appointment.status(),
				appointment.source(),
				appointment.createdAt(),
				appointment.updatedAt()
		);
	}

	public Appointment toDomain(AppointmentJpaEntity entity) {
		return new Appointment(
				entity.getId(),
				entity.getCustomerId(),
				entity.getBarberId(),
				entity.getServiceOfferingId(),
				entity.getStartAt(),
				entity.getEndAt(),
				entity.getStatus(),
				entity.getSource(),
				entity.getCreatedAt(),
				entity.getUpdatedAt()
		);
	}
}
