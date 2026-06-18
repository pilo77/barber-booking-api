package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.ServiceOfferingResponse;
import com.villamil.barberbooking.application.port.in.DeactivateServiceOfferingUseCase;
import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;
import com.villamil.barberbooking.domain.exception.ServiceOfferingNotFoundException;
import com.villamil.barberbooking.domain.model.ServiceOffering;

@Service
class DeactivateServiceOfferingService implements DeactivateServiceOfferingUseCase {

	private final ServiceOfferingRepositoryPort serviceOfferingRepositoryPort;

	DeactivateServiceOfferingService(ServiceOfferingRepositoryPort serviceOfferingRepositoryPort) {
		this.serviceOfferingRepositoryPort = serviceOfferingRepositoryPort;
	}

	@Override
	@Transactional
	public ServiceOfferingResponse deactivate(Long id) {
		ServiceOffering serviceOffering = serviceOfferingRepositoryPort.findById(id)
				.orElseThrow(() -> new ServiceOfferingNotFoundException("Service offering not found"));

		return ServiceOfferingResponse.from(serviceOfferingRepositoryPort.save(serviceOffering.deactivate()));
	}
}
