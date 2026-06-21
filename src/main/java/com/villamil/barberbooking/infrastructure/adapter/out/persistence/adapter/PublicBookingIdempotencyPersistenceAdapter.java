package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.util.UUID;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.idempotency.PublicBookingIdempotencyRecord;
import com.villamil.barberbooking.application.idempotency.PublicBookingIdempotencyRecord.Status;
import com.villamil.barberbooking.application.port.out.PublicBookingIdempotencyPort;
import com.villamil.barberbooking.application.port.out.TenantContextProvider;
import com.villamil.barberbooking.application.service.PublicBookingIdempotencyProperties;
import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.PublicBookingIdempotencyJpaRepository;

@Component
public class PublicBookingIdempotencyPersistenceAdapter implements PublicBookingIdempotencyPort {

	private final PublicBookingIdempotencyJpaRepository repository;
	private final TenantContextProvider tenantContextProvider;
	private final PublicBookingIdempotencyProperties properties;

	public PublicBookingIdempotencyPersistenceAdapter(
			PublicBookingIdempotencyJpaRepository repository,
			TenantContextProvider tenantContextProvider,
			PublicBookingIdempotencyProperties properties
	) {
		this.repository = repository;
		this.tenantContextProvider = tenantContextProvider;
		this.properties = properties;
	}

	@Override
	public String tryStart(String idempotencyKey, String requestHash) {
		TenantContext tenant = tenantContextProvider.currentTenant();
		String claimToken = UUID.randomUUID().toString();
		return repository.tryStart(
				tenant.companyId(),
				tenant.branchId(),
				idempotencyKey,
				requestHash,
				claimToken,
				properties.getInProgressTtlSeconds()
		) == 1 ? claimToken : null;
	}

	@Override
	public Optional<PublicBookingIdempotencyRecord> find(String idempotencyKey) {
		TenantContext tenant = tenantContextProvider.currentTenant();
		return repository.findByCompanyIdAndBranchIdAndIdempotencyKey(
				tenant.companyId(), tenant.branchId(), idempotencyKey
		)
				.map(entity -> new PublicBookingIdempotencyRecord(
						entity.getRequestHash(),
						entity.getAppointmentId(),
						Status.valueOf(entity.getStatus())
				));
	}

	@Override
	public void complete(String idempotencyKey, String claimToken, Long appointmentId) {
		TenantContext tenant = tenantContextProvider.currentTenant();
		int updated = repository.complete(
				tenant.companyId(), tenant.branchId(), idempotencyKey, claimToken, appointmentId
		);
		if (updated != 1) {
			throw new IllegalStateException("Public booking idempotency record could not be completed");
		}
	}
}
