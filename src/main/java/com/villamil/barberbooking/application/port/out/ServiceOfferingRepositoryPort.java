package com.villamil.barberbooking.application.port.out;

import java.util.List;
import java.util.Optional;

import com.villamil.barberbooking.domain.model.ServiceOffering;

public interface ServiceOfferingRepositoryPort {

	ServiceOffering save(ServiceOffering serviceOffering);

	Optional<ServiceOffering> findById(Long id);

	List<ServiceOffering> findAll();

	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, Long id);
}
