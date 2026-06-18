package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.CreatePublicAppointmentCommand;
import com.villamil.barberbooking.application.dto.response.PublicAppointmentResponse;

public interface CreatePublicAppointmentUseCase {

	PublicAppointmentResponse create(CreatePublicAppointmentCommand command);
}
