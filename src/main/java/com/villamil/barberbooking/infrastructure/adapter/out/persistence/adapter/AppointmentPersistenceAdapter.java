package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.application.port.out.TenantContextProvider;
import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.domain.model.Appointment;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper.AppointmentPersistenceMapper;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.AppointmentJpaRepository;

@Component
public class AppointmentPersistenceAdapter implements AppointmentRepositoryPort {

	private static final List<AppointmentStatus> BLOCKING_STATUSES = List.of(
			AppointmentStatus.SCHEDULED,
			AppointmentStatus.IN_PROGRESS
	);

	private final AppointmentJpaRepository appointmentJpaRepository;
	private final AppointmentPersistenceMapper appointmentPersistenceMapper;
	private final TenantContextProvider tenantContextProvider;

	public AppointmentPersistenceAdapter(
			AppointmentJpaRepository appointmentJpaRepository,
			AppointmentPersistenceMapper appointmentPersistenceMapper,
			TenantContextProvider tenantContextProvider
	) {
		this.appointmentJpaRepository = appointmentJpaRepository;
		this.appointmentPersistenceMapper = appointmentPersistenceMapper;
		this.tenantContextProvider = tenantContextProvider;
	}

	@Override
	public Appointment save(Appointment appointment) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return appointmentPersistenceMapper.toDomain(
				appointmentJpaRepository.save(appointmentPersistenceMapper.toEntity(appointment, tenantContext))
		);
	}

	@Override
	public Optional<Appointment> findById(Long id) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return appointmentJpaRepository.findByIdAndCompanyIdAndBranchId(
				id,
				tenantContext.companyId(),
				tenantContext.branchId()
		)
				.map(appointmentPersistenceMapper::toDomain);
	}

	@Override
	public boolean existsBlockingOverlap(Long barberId, LocalDateTime startAt, LocalDateTime endAt) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return appointmentJpaRepository.existsBlockingOverlap(
				tenantContext.companyId(),
				tenantContext.branchId(),
				barberId,
				startAt,
				endAt,
				BLOCKING_STATUSES
		);
	}

	@Override
	public List<Appointment> findByBarberIdAndDate(Long barberId, LocalDate date) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return appointmentJpaRepository
				.findAllByCompanyIdAndBranchIdAndBarberIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
						tenantContext.companyId(),
						tenantContext.branchId(),
						barberId,
						date.atStartOfDay(),
						date.plusDays(1).atStartOfDay()
				)
				.stream()
				.map(appointmentPersistenceMapper::toDomain)
				.toList();
	}
}
