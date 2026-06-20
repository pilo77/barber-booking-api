package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.villamil.barberbooking.application.idempotency.PublicBookingIdempotencyRecord;
import com.villamil.barberbooking.application.port.out.TenantContextProvider;
import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.PublicBookingIdempotencyJpaEntity;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.PublicBookingIdempotencyJpaRepository;

@ExtendWith(MockitoExtension.class)
class PublicBookingIdempotencyPersistenceAdapterTest {

	@Mock
	private PublicBookingIdempotencyJpaRepository repository;

	@Mock
	private TenantContextProvider tenantContextProvider;

	@Mock
	private PublicBookingIdempotencyJpaEntity entity;

	private PublicBookingIdempotencyPersistenceAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new PublicBookingIdempotencyPersistenceAdapter(repository, tenantContextProvider);
		when(tenantContextProvider.currentTenant()).thenReturn(new TenantContext(7L, 9L));
	}

	@Test
	void claimIsScopedByCurrentTenant() {
		when(repository.tryStart(7L, 9L, "booking-key-123", "request-hash")).thenReturn(1);

		assertThat(adapter.tryStart("booking-key-123", "request-hash")).isTrue();
		verify(repository).tryStart(7L, 9L, "booking-key-123", "request-hash");
	}

	@Test
	void sameKeyCanBeClaimedByDifferentBranches() {
		when(tenantContextProvider.currentTenant())
				.thenReturn(new TenantContext(7L, 9L), new TenantContext(7L, 10L));
		when(repository.tryStart(7L, 9L, "shared-key-123", "request-hash")).thenReturn(1);
		when(repository.tryStart(7L, 10L, "shared-key-123", "request-hash")).thenReturn(1);

		assertThat(adapter.tryStart("shared-key-123", "request-hash")).isTrue();
		assertThat(adapter.tryStart("shared-key-123", "request-hash")).isTrue();
		verify(repository).tryStart(7L, 9L, "shared-key-123", "request-hash");
		verify(repository).tryStart(7L, 10L, "shared-key-123", "request-hash");
	}

	@Test
	void existingCompletedRecordIsMapped() {
		when(entity.getRequestHash()).thenReturn("request-hash");
		when(entity.getAppointmentId()).thenReturn(15L);
		when(entity.getStatus()).thenReturn("COMPLETED");
		when(repository.findByCompanyIdAndBranchIdAndIdempotencyKey(7L, 9L, "booking-key-123"))
				.thenReturn(Optional.of(entity));

		Optional<PublicBookingIdempotencyRecord> result = adapter.find("booking-key-123");

		assertThat(result).contains(new PublicBookingIdempotencyRecord(
				"request-hash", 15L, PublicBookingIdempotencyRecord.Status.COMPLETED
		));
	}

	@Test
	void completingMissingClaimFailsFast() {
		when(repository.complete(7L, 9L, "booking-key-123", 15L)).thenReturn(0);

		assertThatThrownBy(() -> adapter.complete("booking-key-123", 15L))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("could not be completed");
	}
}
