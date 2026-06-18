package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.ServiceOfferingResponse;
import com.villamil.barberbooking.application.port.in.GetServiceOfferingUseCase;
import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;
import com.villamil.barberbooking.domain.exception.ServiceOfferingNotFoundException;

@Service
class GetServiceOfferingService implements GetServiceOfferingUseCase {

	private final ServiceOfferingRepositoryPort serviceOfferingRepositoryPort;

	GetServiceOfferingService(ServiceOfferingRepositoryPort serviceOfferingRepositoryPort) {
		this.serviceOfferingRepositoryPort = serviceOfferingRepositoryPort;
	}

	@Override
	@Transactional(readOnly = true)
	public ServiceOfferingResponse getById(Long id) {
		return serviceOfferingRepositoryPort.findById(id)
				.map(ServiceOfferingResponse::from)
				.orElseThrow(() -> new ServiceOfferingNotFoundException("Service offering not found"));
	}
}
