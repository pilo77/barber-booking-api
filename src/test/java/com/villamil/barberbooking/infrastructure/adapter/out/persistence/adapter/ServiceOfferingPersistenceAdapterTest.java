package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.villamil.barberbooking.application.port.out.TenantContextProvider;
import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper.ServiceOfferingPersistenceMapper;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.ServiceOfferingJpaRepository;

class ServiceOfferingPersistenceAdapterTest {

	private final ServiceOfferingJpaRepository repository = org.mockito.Mockito.mock(ServiceOfferingJpaRepository.class);
	private final TenantContextProvider tenantContextProvider = org.mockito.Mockito.mock(TenantContextProvider.class);
	private final ServiceOfferingPersistenceAdapter adapter = new ServiceOfferingPersistenceAdapter(
			repository,
			new ServiceOfferingPersistenceMapper(),
			tenantContextProvider
	);

	@Test
	void shouldAllowSameServiceNameAcrossDifferentCompaniesByCheckingCurrentCompanyOnly() {
		when(tenantContextProvider.currentTenant()).thenReturn(new TenantContext(2L, 1L));

		adapter.existsByName("Corte clasico");

		verify(repository).existsByCompanyIdAndName(2L, "Corte clasico");
	}
}
