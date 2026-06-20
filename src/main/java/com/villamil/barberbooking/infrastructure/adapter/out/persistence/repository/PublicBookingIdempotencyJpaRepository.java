package com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.PublicBookingIdempotencyJpaEntity;

public interface PublicBookingIdempotencyJpaRepository
		extends JpaRepository<PublicBookingIdempotencyJpaEntity, Long> {

	@Modifying(flushAutomatically = true)
	@Query(value = """
			INSERT INTO public_booking_idempotency_keys (
				company_id, branch_id, idempotency_key, request_hash, status
			) VALUES (:companyId, :branchId, :idempotencyKey, :requestHash, 'IN_PROGRESS')
			ON CONFLICT (company_id, branch_id, idempotency_key) DO NOTHING
			""", nativeQuery = true)
	int tryStart(Long companyId, Long branchId, String idempotencyKey, String requestHash);

	Optional<PublicBookingIdempotencyJpaEntity> findByCompanyIdAndBranchIdAndIdempotencyKey(
			Long companyId,
			Long branchId,
			String idempotencyKey
	);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update PublicBookingIdempotencyJpaEntity record
			set record.status = 'COMPLETED',
				record.appointmentId = :appointmentId,
				record.completedAt = CURRENT_TIMESTAMP
			where record.companyId = :companyId
				and record.branchId = :branchId
				and record.idempotencyKey = :idempotencyKey
				and record.status = 'IN_PROGRESS'
			""")
	int complete(Long companyId, Long branchId, String idempotencyKey, Long appointmentId);
}
