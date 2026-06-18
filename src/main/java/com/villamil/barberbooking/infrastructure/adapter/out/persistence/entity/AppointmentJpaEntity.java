package com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.time.LocalDateTime;

import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointments")
public class AppointmentJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "company_id", nullable = false)
	private Long companyId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "customer_id", nullable = false)
	private Long customerId;

	@Column(name = "barber_id", nullable = false)
	private Long barberId;

	@Column(name = "service_offering_id", nullable = false)
	private Long serviceOfferingId;

	@Column(name = "start_at", nullable = false)
	private LocalDateTime startAt;

	@Column(name = "end_at", nullable = false)
	private LocalDateTime endAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private AppointmentStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false, length = 30)
	private AppointmentSource source;

	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant createdAt;

	@Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
	private Instant updatedAt;

	protected AppointmentJpaEntity() {
	}

	public AppointmentJpaEntity(
			Long id,
			Long companyId,
			Long branchId,
			Long customerId,
			Long barberId,
			Long serviceOfferingId,
			LocalDateTime startAt,
			LocalDateTime endAt,
			AppointmentStatus status,
			AppointmentSource source,
			Instant createdAt,
			Instant updatedAt
	) {
		this.id = id;
		this.companyId = companyId;
		this.branchId = branchId;
		this.customerId = customerId;
		this.barberId = barberId;
		this.serviceOfferingId = serviceOfferingId;
		this.startAt = startAt;
		this.endAt = endAt;
		this.status = status;
		this.source = source;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Long getCompanyId() {
		return companyId;
	}

	public Long getBranchId() {
		return branchId;
	}

	public Long getCustomerId() {
		return customerId;
	}

	public Long getBarberId() {
		return barberId;
	}

	public Long getServiceOfferingId() {
		return serviceOfferingId;
	}

	public LocalDateTime getStartAt() {
		return startAt;
	}

	public LocalDateTime getEndAt() {
		return endAt;
	}

	public AppointmentStatus getStatus() {
		return status;
	}

	public AppointmentSource getSource() {
		return source;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
