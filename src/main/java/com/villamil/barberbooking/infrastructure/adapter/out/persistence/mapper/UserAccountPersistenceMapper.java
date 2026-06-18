package com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.domain.model.UserAccount;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.entity.UserAccountJpaEntity;

@Component
public class UserAccountPersistenceMapper {

	public UserAccountJpaEntity toEntity(UserAccount userAccount) {
		return new UserAccountJpaEntity(
				userAccount.id(),
				userAccount.companyId(),
				userAccount.branchId(),
				userAccount.email(),
				userAccount.passwordHash(),
				userAccount.fullName(),
				userAccount.phone(),
				userAccount.active(),
				userAccount.createdAt(),
				userAccount.updatedAt(),
				userAccount.roles()
		);
	}

	public UserAccount toDomain(UserAccountJpaEntity entity) {
		return new UserAccount(
				entity.getId(),
				entity.getCompanyId(),
				entity.getBranchId(),
				entity.getEmail(),
				entity.getPasswordHash(),
				entity.getFullName(),
				entity.getPhone(),
				entity.isActive(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getRoles()
		);
	}
}
