package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;
import com.villamil.barberbooking.domain.exception.ServiceOfferingAlreadyExistsException;
import com.villamil.barberbooking.domain.model.ServiceOffering;

@Component
class ServiceOfferingUniquenessValidator {

	private final ServiceOfferingRepositoryPort serviceOfferingRepositoryPort;

	ServiceOfferingUniquenessValidator(ServiceOfferingRepositoryPort serviceOfferingRepositoryPort) {
		this.serviceOfferingRepositoryPort = serviceOfferingRepositoryPort;
	}

	void ensureCanCreate(ServiceOffering serviceOffering) {
		if (serviceOfferingRepositoryPort.existsByName(serviceOffering.name())) {
			throw new ServiceOfferingAlreadyExistsException("Service offering name already exists");
		}
	}

	void ensureCanUpdate(ServiceOffering serviceOffering) {
		if (serviceOfferingRepositoryPort.existsByNameAndIdNot(serviceOffering.name(), serviceOffering.id())) {
			throw new ServiceOfferingAlreadyExistsException("Service offering name already exists");
		}
	}
}
