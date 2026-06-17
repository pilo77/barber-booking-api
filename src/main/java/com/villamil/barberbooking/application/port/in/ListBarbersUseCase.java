package com.villamil.barberbooking.application.port.in;

import java.util.List;

import com.villamil.barberbooking.application.dto.response.BarberResponse;

public interface ListBarbersUseCase {

	List<BarberResponse> list();
}
