package com.villamil.barberbooking.application.port.out;

import java.util.List;
import java.util.Optional;

import com.villamil.barberbooking.domain.model.UserAccount;

public interface UserAccountRepositoryPort {

	UserAccount save(UserAccount userAccount);

	boolean existsAny();

	boolean existsByEmail(String email);

	boolean existsByBarberId(Long barberId);

	Optional<UserAccount> findByEmail(String email);

	Optional<UserAccount> findById(Long id);

	List<UserAccount> findAll();

	List<UserAccount> findAllByCompanyId(Long companyId);

	List<UserAccount> findAllByCompanyIdAndBranchId(Long companyId, Long branchId);
}
