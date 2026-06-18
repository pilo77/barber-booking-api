package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;
import com.villamil.barberbooking.application.port.out.TenantContextProvider;
import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.domain.model.BarberWorkingHour;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper.BarberWorkingHourPersistenceMapper;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.BarberWorkingHourJpaRepository;

@Component
public class BarberWorkingHourPersistenceAdapter implements BarberWorkingHourRepositoryPort {

	private final BarberWorkingHourJpaRepository barberWorkingHourJpaRepository;
	private final BarberWorkingHourPersistenceMapper barberWorkingHourPersistenceMapper;
	private final TenantContextProvider tenantContextProvider;

	public BarberWorkingHourPersistenceAdapter(
			BarberWorkingHourJpaRepository barberWorkingHourJpaRepository,
			BarberWorkingHourPersistenceMapper barberWorkingHourPersistenceMapper,
			TenantContextProvider tenantContextProvider
	) {
		this.barberWorkingHourJpaRepository = barberWorkingHourJpaRepository;
		this.barberWorkingHourPersistenceMapper = barberWorkingHourPersistenceMapper;
		this.tenantContextProvider = tenantContextProvider;
	}

	@Override
	public BarberWorkingHour save(BarberWorkingHour workingHour) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return barberWorkingHourPersistenceMapper.toDomain(
				barberWorkingHourJpaRepository.save(barberWorkingHourPersistenceMapper.toEntity(workingHour, tenantContext))
		);
	}

	@Override
	public Optional<BarberWorkingHour> findByIdAndBarberId(Long id, Long barberId) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return barberWorkingHourJpaRepository.findByIdAndBarberIdAndCompanyIdAndBranchId(
				id,
				barberId,
				tenantContext.companyId(),
				tenantContext.branchId()
		)
				.map(barberWorkingHourPersistenceMapper::toDomain);
	}

	@Override
	public List<BarberWorkingHour> findAllByBarberId(Long barberId) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return barberWorkingHourJpaRepository.findAllByBarberIdAndCompanyIdAndBranchIdOrderByDayOfWeekAscStartTimeAsc(
				barberId,
				tenantContext.companyId(),
				tenantContext.branchId()
		)
				.stream()
				.map(barberWorkingHourPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<BarberWorkingHour> findActiveByBarberIdAndDay(Long barberId, DayOfWeek dayOfWeek) {
		TenantContext tenantContext = tenantContextProvider.currentTenant();
		return barberWorkingHourJpaRepository
				.findAllByBarberIdAndCompanyIdAndBranchIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
						barberId,
						tenantContext.companyId(),
						tenantContext.branchId(),
						(short) dayOfWeek.getValue()
				)
				.stream()
				.map(barberWorkingHourPersistenceMapper::toDomain)
				.toList();
	}
}
