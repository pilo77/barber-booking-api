package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.villamil.barberbooking.application.port.out.TenantContextProvider;
import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper.AppointmentPersistenceMapper;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.AppointmentJpaRepository;

class AppointmentPersistenceAdapterTest {

	private final AppointmentJpaRepository repository = org.mockito.Mockito.mock(AppointmentJpaRepository.class);
	private final TenantContextProvider tenantContextProvider = org.mockito.Mockito.mock(TenantContextProvider.class);
	private final AppointmentPersistenceAdapter adapter = new AppointmentPersistenceAdapter(
			repository,
			new AppointmentPersistenceMapper(),
			tenantContextProvider
	);

	@Test
	void shouldCheckBlockingOverlapWithinCurrentTenantOnly() {
		LocalDateTime startAt = LocalDateTime.of(2026, 6, 18, 9, 0);
		LocalDateTime endAt = startAt.plusMinutes(30);
		when(tenantContextProvider.currentTenant()).thenReturn(new TenantContext(2L, 3L));

		adapter.existsBlockingOverlap(4L, startAt, endAt);

		verify(repository).existsBlockingOverlap(
				org.mockito.ArgumentMatchers.eq(2L),
				org.mockito.ArgumentMatchers.eq(3L),
				org.mockito.ArgumentMatchers.eq(4L),
				org.mockito.ArgumentMatchers.eq(startAt),
				org.mockito.ArgumentMatchers.eq(endAt),
				org.mockito.ArgumentMatchers.argThat(statuses ->
						statuses.contains(AppointmentStatus.SCHEDULED)
								&& statuses.contains(AppointmentStatus.IN_PROGRESS))
		);
	}

	@Test
	void shouldLoadDailyAppointmentsWithinCurrentTenantOnly() {
		LocalDate date = LocalDate.of(2026, 6, 18);
		when(tenantContextProvider.currentTenant()).thenReturn(new TenantContext(2L, 3L));

		adapter.findByBarberIdAndDate(4L, date);

		verify(repository).findAllByCompanyIdAndBranchIdAndBarberIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
				2L,
				3L,
				4L,
				date.atStartOfDay(),
				date.plusDays(1).atStartOfDay()
		);
	}
}
