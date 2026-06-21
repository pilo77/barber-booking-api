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
				company_id, branch_id, idempotency_key, request_hash, claim_token, status
			) VALUES (:companyId, :branchId, :idempotencyKey, :requestHash, :claimToken, 'IN_PROGRESS')
			ON CONFLICT (company_id, branch_id, idempotency_key) DO UPDATE
			SET request_hash = EXCLUDED.request_hash,
				claim_token = EXCLUDED.claim_token,
				status = 'IN_PROGRESS',
				appointment_id = NULL,
				completed_at = NULL,
				created_at = CURRENT_TIMESTAMP
			WHERE public_booking_idempotency_keys.status = 'IN_PROGRESS'
				AND public_booking_idempotency_keys.created_at <= (
					CURRENT_TIMESTAMP - make_interval(secs => CAST(:inProgressTtlSeconds AS integer))
				)
			""", nativeQuery = true)
	int tryStart(
			Long companyId,
			Long branchId,
			String idempotencyKey,
			String requestHash,
			String claimToken,
			long inProgressTtlSeconds
	);

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
				and record.claimToken = :claimToken
				and record.status = 'IN_PROGRESS'
			""")
	int complete(Long companyId, Long branchId, String idempotencyKey, String claimToken, Long appointmentId);
}
