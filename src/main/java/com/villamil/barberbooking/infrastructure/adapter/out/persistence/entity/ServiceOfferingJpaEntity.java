package com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity;

import java.math.BigDecimal;
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
@Table(name = "services")
public class ServiceOfferingJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "company_id", nullable = false)
	private Long companyId;

	@Column(name = "name", nullable = false, length = 120)
	private String name;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "duration_minutes", nullable = false)
	private int durationMinutes;

	@Column(name = "price", nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant updatedAt;

	protected ServiceOfferingJpaEntity() {
	}

	public ServiceOfferingJpaEntity(
			Long id,
			Long companyId,
			String name,
			String description,
			int durationMinutes,
			BigDecimal price,
			boolean active,
			Instant createdAt,
			Instant updatedAt
	) {
		this.id = id;
		this.companyId = companyId;
		this.name = name;
		this.description = description;
		this.durationMinutes = durationMinutes;
		this.price = price;
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

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public int getDurationMinutes() {
		return durationMinutes;
	}

	public BigDecimal getPrice() {
		return price;
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
