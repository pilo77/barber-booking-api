package com.villamil.barberbooking.application.port.out;

import java.util.List;
import java.util.Optional;

import com.villamil.barberbooking.domain.model.BarberService;

public interface BarberServiceRepositoryPort {

	BarberService save(BarberService service);

	Optional<BarberService> findById(Long id);

	List<BarberService> findAllActive();
}
