package com.villamil.barberbooking.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.villamil.barberbooking.application.dto.command.GetBarberDailyDashboardCommand;
import com.villamil.barberbooking.application.dto.response.BarberDailyDashboardResponse;
import com.villamil.barberbooking.application.dto.response.BarberDailyDashboardSummaryResponse;
import com.villamil.barberbooking.application.dto.response.DailyAppointmentItemResponse;
import com.villamil.barberbooking.application.port.in.GetBarberDailyDashboardUseCase;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

@ExtendWith(MockitoExtension.class)
class BarberDailyDashboardControllerTest {

	@Mock
	private GetBarberDailyDashboardUseCase getBarberDailyDashboardUseCase;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper()
				.findAndRegisterModules()
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		BarberDailyDashboardController controller = new BarberDailyDashboardController(getBarberDailyDashboardUseCase);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	void getDailyDashboardReturnsOk() throws Exception {
		when(getBarberDailyDashboardUseCase.getDailyDashboard(any(GetBarberDailyDashboardCommand.class)))
				.thenReturn(dashboard());

		mockMvc.perform(get("/api/v1/barbers/2/daily-dashboard?date=2026-06-22"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.barberId").value(2))
				.andExpect(jsonPath("$.barberName").value("Carlos Gomez"))
				.andExpect(jsonPath("$.date").value("2026-06-22"))
				.andExpect(jsonPath("$.summary.totalAppointments").value(2))
				.andExpect(jsonPath("$.summary.scheduled").value(1))
				.andExpect(jsonPath("$.summary.cancelled").value(1))
				.andExpect(jsonPath("$.summary.occupiedMinutes").value(30))
				.andExpect(jsonPath("$.nextAppointment.appointmentId").value(1))
				.andExpect(jsonPath("$.nextAppointment.customerName").value("Ana Perez"))
				.andExpect(jsonPath("$.appointments[0].appointmentId").value(1))
				.andExpect(jsonPath("$.appointments[0].serviceName").value("Corte clasico"))
				.andExpect(jsonPath("$.appointments[0].status").value("SCHEDULED"))
				.andExpect(jsonPath("$.appointments[0].source").value("ONLINE"));
	}

	@Test
	void rejectMissingDate() throws Exception {
		mockMvc.perform(get("/api/v1/barbers/2/daily-dashboard"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request"));

		verify(getBarberDailyDashboardUseCase, never()).getDailyDashboard(any(GetBarberDailyDashboardCommand.class));
	}

	@Test
	void rejectInvalidDate() throws Exception {
		mockMvc.perform(get("/api/v1/barbers/2/daily-dashboard?date=not-a-date"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request"));

		verify(getBarberDailyDashboardUseCase, never()).getDailyDashboard(any(GetBarberDailyDashboardCommand.class));
	}

	@Test
	void missingBarberReturnsNotFound() throws Exception {
		when(getBarberDailyDashboardUseCase.getDailyDashboard(any(GetBarberDailyDashboardCommand.class)))
				.thenThrow(new BarberNotFoundException("Barber not found"));

		mockMvc.perform(get("/api/v1/barbers/99/daily-dashboard?date=2026-06-22"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Barber not found"));
	}

	private BarberDailyDashboardResponse dashboard() {
		DailyAppointmentItemResponse scheduled = new DailyAppointmentItemResponse(
				1L,
				1L,
				"Ana Perez",
				3L,
				"Corte clasico",
				LocalDateTime.of(2026, 6, 22, 9, 0),
				LocalDateTime.of(2026, 6, 22, 9, 30),
				AppointmentStatus.SCHEDULED,
				AppointmentSource.ONLINE
		);
		DailyAppointmentItemResponse cancelled = new DailyAppointmentItemResponse(
				2L,
				2L,
				"Luis Mora",
				3L,
				"Corte clasico",
				LocalDateTime.of(2026, 6, 22, 10, 0),
				LocalDateTime.of(2026, 6, 22, 10, 30),
				AppointmentStatus.CANCELLED,
				AppointmentSource.ONLINE
		);
		return new BarberDailyDashboardResponse(
				2L,
				"Carlos Gomez",
				LocalDate.of(2026, 6, 22),
				new BarberDailyDashboardSummaryResponse(2, 1, 0, 0, 1, 0, 30),
				scheduled,
				List.of(scheduled, cancelled)
		);
	}
}
