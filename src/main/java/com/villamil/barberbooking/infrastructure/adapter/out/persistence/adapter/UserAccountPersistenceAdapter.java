package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.UserAccountRepositoryPort;
import com.villamil.barberbooking.domain.model.UserAccount;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper.UserAccountPersistenceMapper;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.UserAccountJpaRepository;

@Component
public class UserAccountPersistenceAdapter implements UserAccountRepositoryPort {

	private final UserAccountJpaRepository userAccountJpaRepository;
	private final UserAccountPersistenceMapper userAccountPersistenceMapper;

	public UserAccountPersistenceAdapter(
			UserAccountJpaRepository userAccountJpaRepository,
			UserAccountPersistenceMapper userAccountPersistenceMapper
	) {
		this.userAccountJpaRepository = userAccountJpaRepository;
		this.userAccountPersistenceMapper = userAccountPersistenceMapper;
	}

	@Override
	public UserAccount save(UserAccount userAccount) {
		return userAccountPersistenceMapper.toDomain(
				userAccountJpaRepository.save(userAccountPersistenceMapper.toEntity(userAccount))
		);
	}

	@Override
	public boolean existsAny() {
		return userAccountJpaRepository.count() > 0;
	}

	@Override
	public boolean existsByEmail(String email) {
		return userAccountJpaRepository.existsByEmail(email);
	}

	@Override
	public Optional<UserAccount> findByEmail(String email) {
		return userAccountJpaRepository.findByEmail(email)
				.map(userAccountPersistenceMapper::toDomain);
	}

	@Override
	public Optional<UserAccount> findById(Long id) {
		return userAccountJpaRepository.findById(id)
				.map(userAccountPersistenceMapper::toDomain);
	}

	@Override
	public List<UserAccount> findAll() {
		return userAccountJpaRepository.findAll()
				.stream()
				.map(userAccountPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<UserAccount> findAllByCompanyId(Long companyId) {
		return userAccountJpaRepository.findAllByCompanyIdOrderByIdAsc(companyId)
				.stream()
				.map(userAccountPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<UserAccount> findAllByCompanyIdAndBranchId(Long companyId, Long branchId) {
		return userAccountJpaRepository.findAllByCompanyIdAndBranchIdOrderByIdAsc(companyId, branchId)
				.stream()
				.map(userAccountPersistenceMapper::toDomain)
				.toList();
	}
}
