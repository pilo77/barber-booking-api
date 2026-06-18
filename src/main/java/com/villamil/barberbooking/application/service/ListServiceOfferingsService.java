package com.villamil.barberbooking.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.ServiceOfferingResponse;
import com.villamil.barberbooking.application.port.in.ListServiceOfferingsUseCase;
import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;

@Service
class ListServiceOfferingsService implements ListServiceOfferingsUseCase {

	private final ServiceOfferingRepositoryPort serviceOfferingRepositoryPort;

	ListServiceOfferingsService(ServiceOfferingRepositoryPort serviceOfferingRepositoryPort) {
		this.serviceOfferingRepositoryPort = serviceOfferingRepositoryPort;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ServiceOfferingResponse> list() {
		return serviceOfferingRepositoryPort.findAll()
				.stream()
				.map(ServiceOfferingResponse::from)
				.toList();
	}
}
