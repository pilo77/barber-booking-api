package com.villamil.barberbooking.application.port.out;

import java.util.List;
import java.util.Optional;

import com.villamil.barberbooking.domain.model.Customer;

public interface CustomerRepositoryPort {

	Customer save(Customer customer);

	Optional<Customer> findById(Long id);

	List<Customer> findAll();

	boolean existsByPhone(String phone);

	boolean existsByEmail(String email);

	boolean existsByPhoneAndIdNot(String phone, Long id);

	boolean existsByEmailAndIdNot(String email, Long id);
}
