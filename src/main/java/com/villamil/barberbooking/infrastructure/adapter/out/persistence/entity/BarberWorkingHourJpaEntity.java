package com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "barber_working_hours")
public class BarberWorkingHourJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "company_id", nullable = false)
	private Long companyId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "barber_id", nullable = false)
	private Long barberId;

	@Column(name = "day_of_week", nullable = false)
	private short dayOfWeek;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant updatedAt;

	protected BarberWorkingHourJpaEntity() {
	}

	public BarberWorkingHourJpaEntity(
			Long id,
			Long companyId,
			Long branchId,
			Long barberId,
			short dayOfWeek,
			LocalTime startTime,
			LocalTime endTime,
			boolean active,
			Instant createdAt,
			Instant updatedAt
	) {
		this.id = id;
		this.companyId = companyId;
		this.branchId = branchId;
		this.barberId = barberId;
		this.dayOfWeek = dayOfWeek;
		this.startTime = startTime;
		this.endTime = endTime;
		this.active = active;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = createdAt;
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

	public Long getBarberId() {
		return barberId;
	}

	public short getDayOfWeek() {
		return dayOfWeek;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
