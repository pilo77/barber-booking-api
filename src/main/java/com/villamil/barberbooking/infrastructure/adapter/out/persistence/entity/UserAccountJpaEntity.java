package com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.villamil.barberbooking.domain.model.Role;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_accounts")
public class UserAccountJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "company_id")
	private Long companyId;

	@Column(name = "branch_id")
	private Long branchId;

	@Column(name = "email", nullable = false, length = 120)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "full_name", nullable = false, length = 120)
	private String fullName;

	@Column(name = "phone", length = 30)
	private String phone;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant updatedAt;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "user_account_roles",
			joinColumns = @JoinColumn(name = "user_account_id")
	)
	@Column(name = "role", nullable = false, length = 40)
	@Enumerated(EnumType.STRING)
	private Set<Role> roles = new HashSet<>();

	protected UserAccountJpaEntity() {
	}

	public UserAccountJpaEntity(
			Long id,
			Long companyId,
			Long branchId,
			String email,
			String passwordHash,
			String fullName,
			String phone,
			boolean active,
			Instant createdAt,
			Instant updatedAt,
			Set<Role> roles
	) {
		this.id = id;
		this.companyId = companyId;
		this.branchId = branchId;
		this.email = email;
		this.passwordHash = passwordHash;
		this.fullName = fullName;
		this.phone = phone;
		this.active = active;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.roles = new HashSet<>(roles);
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

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getFullName() {
		return fullName;
	}

	public String getPhone() {
		return phone;
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

	public Set<Role> getRoles() {
		return Set.copyOf(roles);
	}
}
