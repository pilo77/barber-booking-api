package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.response.AppointmentResponse;

public interface MarkAppointmentNoShowUseCase {

	AppointmentResponse markNoShow(Long id);
}
