package com.villamil.barberbooking.application.port.out;

import java.util.List;
import java.util.Optional;

import com.villamil.barberbooking.domain.model.Barber;

public interface BarberRepositoryPort {

	Barber save(Barber barber);

	Optional<Barber> findById(Long id);

	List<Barber> findAll();

	boolean existsByPhone(String phone);

	boolean existsByEmail(String email);

	boolean existsByPhoneAndIdNot(String phone, Long id);

	boolean existsByEmailAndIdNot(String email, Long id);
}
