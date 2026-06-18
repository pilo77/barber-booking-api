package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.GetBarberDailyDashboardCommand;
import com.villamil.barberbooking.application.dto.response.BarberDailyDashboardResponse;

public interface GetBarberDailyDashboardUseCase {

	BarberDailyDashboardResponse getDailyDashboard(GetBarberDailyDashboardCommand command);
}
