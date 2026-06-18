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
@Table(name = "companies")
public class CompanyJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 120)
	private String name;

	@Column(name = "slug", nullable = false, length = 120)
	private String slug;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant updatedAt;

	protected CompanyJpaEntity() {
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
}
