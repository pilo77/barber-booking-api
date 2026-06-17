package com.villamil.barberbooking.application.port.out;

import java.util.List;
import java.util.Optional;

import com.villamil.barberbooking.domain.model.Barber;

public interface BarberRepositoryPort {

	Barber save(Barber barber);

	Optional<Barber> findById(Long id);

	List<Barber> findAll();
}
