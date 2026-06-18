package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.villamil.barberbooking.application.port.out.TenantContextProvider;
import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.domain.model.Customer;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.CustomerJpaEntity;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper.CustomerPersistenceMapper;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.CustomerJpaRepository;

class CustomerPersistenceAdapterTest {

	private final CustomerJpaRepository repository = org.mockito.Mockito.mock(CustomerJpaRepository.class);
	private final TenantContextProvider tenantContextProvider = org.mockito.Mockito.mock(TenantContextProvider.class);
	private final CustomerPersistenceAdapter adapter = new CustomerPersistenceAdapter(
			repository,
			new CustomerPersistenceMapper(),
			tenantContextProvider
	);

	@Test
	void shouldSaveCustomerWithCurrentTenant() {
		TenantContext tenantContext = new TenantContext(2L, 1L);
		Customer customer = new Customer(10L, "Ana Perez", "3001234567", "ana@example.com", true, Instant.now(), Instant.now());
		when(tenantContextProvider.currentTenant()).thenReturn(tenantContext);
		when(repository.save(org.mockito.ArgumentMatchers.any(CustomerJpaEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		adapter.save(customer);

		ArgumentCaptor<CustomerJpaEntity> captor = ArgumentCaptor.forClass(CustomerJpaEntity.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getCompanyId()).isEqualTo(2L);
	}

	@Test
	void shouldListCustomersByCurrentCompany() {
		when(tenantContextProvider.currentTenant()).thenReturn(new TenantContext(2L, 1L));

		adapter.findAll();

		verify(repository).findAllByCompanyIdOrderByIdAsc(2L);
	}
}
