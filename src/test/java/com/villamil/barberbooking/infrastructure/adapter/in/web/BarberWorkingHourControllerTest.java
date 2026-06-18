package com.villamil.barberbooking.infrastructure.adapter.in.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.villamil.barberbooking.application.dto.command.CreateBarberWorkingHourCommand;
import com.villamil.barberbooking.application.dto.command.UpdateBarberWorkingHourCommand;
import com.villamil.barberbooking.application.dto.response.BarberWorkingHourResponse;
import com.villamil.barberbooking.application.port.in.ActivateBarberWorkingHourUseCase;
import com.villamil.barberbooking.application.port.in.CreateBarberWorkingHourUseCase;
import com.villamil.barberbooking.application.port.in.DeactivateBarberWorkingHourUseCase;
import com.villamil.barberbooking.application.port.in.GetBarberWorkingHourUseCase;
import com.villamil.barberbooking.application.port.in.ListBarberWorkingHoursUseCase;
import com.villamil.barberbooking.application.port.in.UpdateBarberWorkingHourUseCase;
import com.villamil.barberbooking.domain.exception.BarberWorkingHourNotFoundException;
import com.villamil.barberbooking.domain.exception.BarberWorkingHourOverlapException;

@ExtendWith(MockitoExtension.class)
class BarberWorkingHourControllerTest {

	private static final Instant CREATED_AT = Instant.parse("2026-06-17T12:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-06-17T12:30:00Z");

	@Mock
	private CreateBarberWorkingHourUseCase createBarberWorkingHourUseCase;

	@Mock
	private GetBarberWorkingHourUseCase getBarberWorkingHourUseCase;

	@Mock
	private ListBarberWorkingHoursUseCase listBarberWorkingHoursUseCase;

	@Mock
	private UpdateBarberWorkingHourUseCase updateBarberWorkingHourUseCase;

	@Mock
	private ActivateBarberWorkingHourUseCase activateBarberWorkingHourUseCase;

	@Mock
	private DeactivateBarberWorkingHourUseCase deactivateBarberWorkingHourUseCase;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper()
				.findAndRegisterModules()
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		BarberWorkingHourController controller = new BarberWorkingHourController(
				createBarberWorkingHourUseCase,
				getBarberWorkingHourUseCase,
				listBarberWorkingHoursUseCase,
				updateBarberWorkingHourUseCase,
				activateBarberWorkingHourUseCase,
				deactivateBarberWorkingHourUseCase
		);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	void createWorkingHourReturnsCreated() throws Exception {
		when(createBarberWorkingHourUseCase.create(any(Long.class), any(CreateBarberWorkingHourCommand.class)))
				.thenReturn(workingHour(true));

		mockMvc.perform(post("/api/v1/barbers/1/working-hours")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "dayOfWeek": "MONDAY",
								  "startTime": "08:00:00",
								  "endTime": "12:00:00"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/barbers/1/working-hours/1"))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void rejectInvalidCreateWorkingHourRequest() throws Exception {
		mockMvc.perform(post("/api/v1/barbers/1/working-hours")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "dayOfWeek": null,
								  "startTime": null,
								  "endTime": null
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request"));

		verify(createBarberWorkingHourUseCase, never()).create(any(Long.class), any(CreateBarberWorkingHourCommand.class));
	}

	@Test
	void listWorkingHoursReturnsOk() throws Exception {
		when(listBarberWorkingHoursUseCase.list(1L)).thenReturn(List.of(workingHour(true)));

		mockMvc.perform(get("/api/v1/barbers/1/working-hours"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].barberId").value(1));
	}

	@Test
	void getWorkingHourReturnsOk() throws Exception {
		when(getBarberWorkingHourUseCase.getById(1L, 1L)).thenReturn(workingHour(true));

		mockMvc.perform(get("/api/v1/barbers/1/working-hours/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.startTime").value("08:00:00"));
	}

	@Test
	void getMissingWorkingHourReturnsNotFound() throws Exception {
		when(getBarberWorkingHourUseCase.getById(1L, 99L))
				.thenThrow(new BarberWorkingHourNotFoundException("Working hour not found"));

		mockMvc.perform(get("/api/v1/barbers/1/working-hours/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Working hour not found"));
	}

	@Test
	void updateWorkingHourReturnsOk() throws Exception {
		when(updateBarberWorkingHourUseCase.update(any(Long.class), any(Long.class), any(UpdateBarberWorkingHourCommand.class)))
				.thenReturn(workingHour(true));

		mockMvc.perform(put("/api/v1/barbers/1/working-hours/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "dayOfWeek": "MONDAY",
								  "startTime": "08:00:00",
								  "endTime": "12:00:00"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void overlapReturnsConflict() throws Exception {
		when(createBarberWorkingHourUseCase.create(any(Long.class), any(CreateBarberWorkingHourCommand.class)))
				.thenThrow(new BarberWorkingHourOverlapException("Working hour overlaps with an active working hour"));

		mockMvc.perform(post("/api/v1/barbers/1/working-hours")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "dayOfWeek": "MONDAY",
								  "startTime": "11:00:00",
								  "endTime": "15:00:00"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Working hour overlap"));
	}

	@Test
	void activateWorkingHourReturnsOk() throws Exception {
		when(activateBarberWorkingHourUseCase.activate(1L, 1L)).thenReturn(workingHour(true));

		mockMvc.perform(patch("/api/v1/barbers/1/working-hours/1/activate"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void deactivateWorkingHourReturnsOk() throws Exception {
		when(deactivateBarberWorkingHourUseCase.deactivate(1L, 1L)).thenReturn(workingHour(false));

		mockMvc.perform(patch("/api/v1/barbers/1/working-hours/1/deactivate"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
	}

	private BarberWorkingHourResponse workingHour(boolean active) {
		return new BarberWorkingHourResponse(
				1L,
				1L,
				DayOfWeek.MONDAY,
				LocalTime.of(8, 0),
				LocalTime.of(12, 0),
				active,
				CREATED_AT,
				UPDATED_AT
		);
	}
}
