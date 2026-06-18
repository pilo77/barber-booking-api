package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.UpdateServiceOfferingCommand;
import com.villamil.barberbooking.application.dto.response.ServiceOfferingResponse;
import com.villamil.barberbooking.application.port.in.UpdateServiceOfferingUseCase;
import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;
import com.villamil.barberbooking.domain.exception.ServiceOfferingNotFoundException;
import com.villamil.barberbooking.domain.model.ServiceOffering;

@Service
class UpdateServiceOfferingService implements UpdateServiceOfferingUseCase {

	private final ServiceOfferingRepositoryPort serviceOfferingRepositoryPort;
	private final ServiceOfferingUniquenessValidator serviceOfferingUniquenessValidator;

	UpdateServiceOfferingService(
			ServiceOfferingRepositoryPort serviceOfferingRepositoryPort,
			ServiceOfferingUniquenessValidator serviceOfferingUniquenessValidator
	) {
		this.serviceOfferingRepositoryPort = serviceOfferingRepositoryPort;
		this.serviceOfferingUniquenessValidator = serviceOfferingUniquenessValidator;
	}

	@Override
	@Transactional
	public ServiceOfferingResponse update(Long id, UpdateServiceOfferingCommand command) {
		ServiceOffering currentServiceOffering = serviceOfferingRepositoryPort.findById(id)
				.orElseThrow(() -> new ServiceOfferingNotFoundException("Service offering not found"));
		ServiceOffering updatedServiceOffering = currentServiceOffering.update(
				command.name(),
				command.description(),
				command.durationMinutes(),
				command.price()
		);

		serviceOfferingUniquenessValidator.ensureCanUpdate(updatedServiceOffering);

		return ServiceOfferingResponse.from(serviceOfferingRepositoryPort.save(updatedServiceOffering));
	}
}
