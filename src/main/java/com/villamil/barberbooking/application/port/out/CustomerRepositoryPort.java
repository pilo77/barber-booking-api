package com.villamil.barberbooking.application.port.out;

import java.util.List;
import java.util.Optional;

import com.villamil.barberbooking.domain.model.Customer;

public interface CustomerRepositoryPort {

	Customer save(Customer customer);

	Optional<Customer> findById(Long id);

	List<Customer> findAll();
}
