package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.response.BarberResponse;

public interface DeactivateBarberUseCase {

	BarberResponse deactivate(Long id);
}
