package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.response.AppointmentResponse;

public interface StartAppointmentUseCase {

	AppointmentResponse start(Long id);
}
