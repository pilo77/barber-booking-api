package com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "barbers")
public class BarberJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "company_id", nullable = false)
	private Long companyId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "full_name", nullable = false, length = 120)
	private String fullName;

	@Column(name = "phone", nullable = false, length = 30)
	private String phone;

	@Column(name = "email", length = 120)
	private String email;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant updatedAt;

	protected BarberJpaEntity() {
	}

	public BarberJpaEntity(
			Long id,
			Long companyId,
			Long branchId,
			String fullName,
			String phone,
			String email,
			boolean active,
			Instant createdAt,
			Instant updatedAt
	) {
		this.id = id;
		this.companyId = companyId;
		this.branchId = branchId;
		this.fullName = fullName;
		this.phone = phone;
		this.email = email;
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

	public String getFullName() {
		return fullName;
	}

	public String getPhone() {
		return phone;
	}

	public String getEmail() {
		return email;
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
