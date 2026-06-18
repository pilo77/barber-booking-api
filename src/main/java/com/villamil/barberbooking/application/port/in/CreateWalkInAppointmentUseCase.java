package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.CreateWalkInAppointmentCommand;
import com.villamil.barberbooking.application.dto.response.AppointmentResponse;

public interface CreateWalkInAppointmentUseCase {

	AppointmentResponse create(CreateWalkInAppointmentCommand command);
}
