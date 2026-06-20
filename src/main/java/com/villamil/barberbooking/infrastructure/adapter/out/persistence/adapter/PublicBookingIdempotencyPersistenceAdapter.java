package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.idempotency.PublicBookingIdempotencyRecord;
import com.villamil.barberbooking.application.idempotency.PublicBookingIdempotencyRecord.Status;
import com.villamil.barberbooking.application.port.out.PublicBookingIdempotencyPort;
import com.villamil.barberbooking.application.port.out.TenantContextProvider;
import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.PublicBookingIdempotencyJpaRepository;

@Component
public class PublicBookingIdempotencyPersistenceAdapter implements PublicBookingIdempotencyPort {

	private final PublicBookingIdempotencyJpaRepository repository;
	private final TenantContextProvider tenantContextProvider;

	public PublicBookingIdempotencyPersistenceAdapter(
			PublicBookingIdempotencyJpaRepository repository,
			TenantContextProvider tenantContextProvider
	) {
		this.repository = repository;
		this.tenantContextProvider = tenantContextProvider;
	}

	@Override
	public boolean tryStart(String idempotencyKey, String requestHash) {
		TenantContext tenant = tenantContextProvider.currentTenant();
		return repository.tryStart(
				tenant.companyId(), tenant.branchId(), idempotencyKey, requestHash
		) == 1;
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
	public void complete(String idempotencyKey, Long appointmentId) {
		TenantContext tenant = tenantContextProvider.currentTenant();
		int updated = repository.complete(
				tenant.companyId(), tenant.branchId(), idempotencyKey, appointmentId
		);
		if (updated != 1) {
			throw new IllegalStateException("Public booking idempotency record could not be completed");
		}
	}
}
