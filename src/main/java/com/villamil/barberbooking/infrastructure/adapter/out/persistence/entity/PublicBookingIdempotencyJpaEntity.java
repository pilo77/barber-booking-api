package com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "public_booking_idempotency_keys")
public class PublicBookingIdempotencyJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "company_id", nullable = false)
	private Long companyId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "idempotency_key", nullable = false, length = 128)
	private String idempotencyKey;

	@Column(name = "request_hash", nullable = false, length = 64)
	private String requestHash;

	@Column(name = "appointment_id")
	private Long appointmentId;

	@Column(name = "status", nullable = false, length = 30)
	private String status;

	@Column(name = "claim_token", nullable = false, length = 64)
	private String claimToken;

	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant createdAt;

	@Column(name = "completed_at", columnDefinition = "TIMESTAMPTZ")
	private Instant completedAt;

	protected PublicBookingIdempotencyJpaEntity() {
	}

	public String getRequestHash() {
		return requestHash;
	}

	public Long getAppointmentId() {
		return appointmentId;
	}

	public String getStatus() {
		return status;
	}
}
