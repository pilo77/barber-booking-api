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
import com.villamil.barberbooking.application.dto.command.GetBarberAvailabilityCommand;
import com.villamil.barberbooking.application.dto.response.AvailabilitySlotResponse;
import com.villamil.barberbooking.application.dto.response.BarberAvailabilityResponse;
import com.villamil.barberbooking.application.port.in.GetBarberAvailabilityUseCase;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;
import com.villamil.barberbooking.domain.exception.ResourceInactiveException;

@ExtendWith(MockitoExtension.class)
class BarberAvailabilityControllerTest {

	@Mock
	private GetBarberAvailabilityUseCase getBarberAvailabilityUseCase;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper()
				.findAndRegisterModules()
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		BarberAvailabilityController controller = new BarberAvailabilityController(getBarberAvailabilityUseCase);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	void getAvailabilityReturnsOk() throws Exception {
		when(getBarberAvailabilityUseCase.getAvailability(any(GetBarberAvailabilityCommand.class)))
				.thenReturn(availability());

		mockMvc.perform(get("/api/v1/barbers/2/availability?date=2026-06-22&serviceOfferingId=3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.barberId").value(2))
				.andExpect(jsonPath("$.serviceOfferingId").value(3))
				.andExpect(jsonPath("$.date").value("2026-06-22"))
				.andExpect(jsonPath("$.slots[0].startAt").value("2026-06-22T08:00:00"))
				.andExpect(jsonPath("$.slots[0].endAt").value("2026-06-22T08:30:00"))
				.andExpect(jsonPath("$.slots[0].status").value("AVAILABLE"))
				.andExpect(jsonPath("$.slots[0].color").value("GREEN"))
				.andExpect(jsonPath("$.slots[0].available").value(true))
				.andExpect(jsonPath("$.slots[1].status").value("OCCUPIED"))
				.andExpect(jsonPath("$.slots[1].color").value("RED"))
				.andExpect(jsonPath("$.slots[1].available").value(false));
	}

	@Test
	void rejectMissingDate() throws Exception {
		mockMvc.perform(get("/api/v1/barbers/2/availability?serviceOfferingId=3"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request"));

		verify(getBarberAvailabilityUseCase, never()).getAvailability(any(GetBarberAvailabilityCommand.class));
	}

	@Test
	void rejectInvalidServiceOfferingId() throws Exception {
		mockMvc.perform(get("/api/v1/barbers/2/availability?date=2026-06-22&serviceOfferingId=0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request"));

		verify(getBarberAvailabilityUseCase, never()).getAvailability(any(GetBarberAvailabilityCommand.class));
	}

	@Test
	void missingBarberReturnsNotFound() throws Exception {
		when(getBarberAvailabilityUseCase.getAvailability(any(GetBarberAvailabilityCommand.class)))
				.thenThrow(new BarberNotFoundException("Barber not found"));

		mockMvc.perform(get("/api/v1/barbers/99/availability?date=2026-06-22&serviceOfferingId=3"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Barber not found"));
	}

	@Test
	void inactiveResourceReturnsConflict() throws Exception {
		when(getBarberAvailabilityUseCase.getAvailability(any(GetBarberAvailabilityCommand.class)))
				.thenThrow(new ResourceInactiveException("Barber must be active"));

		mockMvc.perform(get("/api/v1/barbers/2/availability?date=2026-06-22&serviceOfferingId=3"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Resource inactive"));
	}

	private BarberAvailabilityResponse availability() {
		return new BarberAvailabilityResponse(
				2L,
				3L,
				LocalDate.of(2026, 6, 22),
				List.of(
						AvailabilitySlotResponse.available(
								LocalDateTime.of(2026, 6, 22, 8, 0),
								LocalDateTime.of(2026, 6, 22, 8, 30)
						),
						AvailabilitySlotResponse.occupied(
								LocalDateTime.of(2026, 6, 22, 8, 30),
								LocalDateTime.of(2026, 6, 22, 9, 0)
						)
				)
		);
	}
}
