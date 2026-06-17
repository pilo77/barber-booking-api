package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.BookAppointmentCommand;
import com.villamil.barberbooking.application.dto.response.AppointmentResponse;

public interface BookAppointmentUseCase {

	AppointmentResponse book(BookAppointmentCommand command);
}
