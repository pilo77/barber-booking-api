package com.villamil.barberbooking.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.BarberResponse;
import com.villamil.barberbooking.application.port.in.ListBarbersUseCase;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;

@Service
class ListBarbersService implements ListBarbersUseCase {

	private final BarberRepositoryPort barberRepositoryPort;

	ListBarbersService(BarberRepositoryPort barberRepositoryPort) {
		this.barberRepositoryPort = barberRepositoryPort;
	}

	@Override
	@Transactional(readOnly = true)
	public List<BarberResponse> list() {
		return barberRepositoryPort.findAll()
				.stream()
				.map(BarberResponse::from)
				.toList();
	}
}
