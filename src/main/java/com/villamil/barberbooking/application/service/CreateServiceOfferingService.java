package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.CreateServiceOfferingCommand;
import com.villamil.barberbooking.application.dto.response.ServiceOfferingResponse;
import com.villamil.barberbooking.application.port.in.CreateServiceOfferingUseCase;
import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;
import com.villamil.barberbooking.domain.model.ServiceOffering;

@Service
class CreateServiceOfferingService implements CreateServiceOfferingUseCase {

	private final ServiceOfferingRepositoryPort serviceOfferingRepositoryPort;
	private final ServiceOfferingUniquenessValidator serviceOfferingUniquenessValidator;

	CreateServiceOfferingService(
			ServiceOfferingRepositoryPort serviceOfferingRepositoryPort,
			ServiceOfferingUniquenessValidator serviceOfferingUniquenessValidator
	) {
		this.serviceOfferingRepositoryPort = serviceOfferingRepositoryPort;
		this.serviceOfferingUniquenessValidator = serviceOfferingUniquenessValidator;
	}

	@Override
	@Transactional
	public ServiceOfferingResponse create(CreateServiceOfferingCommand command) {
		ServiceOffering serviceOffering = ServiceOffering.create(
				command.name(),
				command.description(),
				command.durationMinutes(),
				command.price()
		);

		serviceOfferingUniquenessValidator.ensureCanCreate(serviceOffering);

		return ServiceOfferingResponse.from(serviceOfferingRepositoryPort.save(serviceOffering));
	}
}
