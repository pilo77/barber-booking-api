package com.villamil.barberbooking.application.port.in;

import java.util.List;

import com.villamil.barberbooking.application.dto.response.BarberWorkingHourResponse;

public interface ListBarberWorkingHoursUseCase {

	List<BarberWorkingHourResponse> list(Long barberId);
}
