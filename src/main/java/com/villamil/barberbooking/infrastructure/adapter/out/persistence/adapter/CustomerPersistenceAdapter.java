package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.domain.model.Customer;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.mapper.CustomerPersistenceMapper;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.repository.CustomerJpaRepository;

@Component
public class CustomerPersistenceAdapter implements CustomerRepositoryPort {

	private final CustomerJpaRepository customerJpaRepository;
	private final CustomerPersistenceMapper customerPersistenceMapper;

	public CustomerPersistenceAdapter(
			CustomerJpaRepository customerJpaRepository,
			CustomerPersistenceMapper customerPersistenceMapper
	) {
		this.customerJpaRepository = customerJpaRepository;
		this.customerPersistenceMapper = customerPersistenceMapper;
	}

	@Override
	public Customer save(Customer customer) {
		return customerPersistenceMapper.toDomain(
				customerJpaRepository.save(customerPersistenceMapper.toEntity(customer))
		);
	}

	@Override
	public Optional<Customer> findById(Long id) {
		return customerJpaRepository.findById(id)
				.map(customerPersistenceMapper::toDomain);
	}

	@Override
	public List<Customer> findAll() {
		return customerJpaRepository.findAll()
				.stream()
				.map(customerPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsByPhone(String phone) {
		return customerJpaRepository.existsByPhone(phone);
	}

	@Override
	public boolean existsByEmail(String email) {
		return customerJpaRepository.existsByEmail(email);
	}

	@Override
	public boolean existsByPhoneAndIdNot(String phone, Long id) {
		return customerJpaRepository.existsByPhoneAndIdNot(phone, id);
	}

	@Override
	public boolean existsByEmailAndIdNot(String email, Long id) {
		return customerJpaRepository.existsByEmailAndIdNot(email, id);
	}
}
