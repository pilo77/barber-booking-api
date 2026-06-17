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

import java.time.Instant;
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
import com.villamil.barberbooking.application.dto.command.CreateBarberCommand;
import com.villamil.barberbooking.application.dto.command.UpdateBarberCommand;
import com.villamil.barberbooking.application.dto.response.BarberResponse;
import com.villamil.barberbooking.application.port.in.ActivateBarberUseCase;
import com.villamil.barberbooking.application.port.in.CreateBarberUseCase;
import com.villamil.barberbooking.application.port.in.DeactivateBarberUseCase;
import com.villamil.barberbooking.application.port.in.GetBarberUseCase;
import com.villamil.barberbooking.application.port.in.ListBarbersUseCase;
import com.villamil.barberbooking.application.port.in.UpdateBarberUseCase;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;

@ExtendWith(MockitoExtension.class)
class BarberControllerTest {

	private static final Instant CREATED_AT = Instant.parse("2026-06-17T12:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-06-17T12:30:00Z");

	@Mock
	private CreateBarberUseCase createBarberUseCase;

	@Mock
	private GetBarberUseCase getBarberUseCase;

	@Mock
	private ListBarbersUseCase listBarbersUseCase;

	@Mock
	private UpdateBarberUseCase updateBarberUseCase;

	@Mock
	private ActivateBarberUseCase activateBarberUseCase;

	@Mock
	private DeactivateBarberUseCase deactivateBarberUseCase;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper().findAndRegisterModules();
		BarberController controller = new BarberController(
				createBarberUseCase,
				getBarberUseCase,
				listBarbersUseCase,
				updateBarberUseCase,
				activateBarberUseCase,
				deactivateBarberUseCase
		);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	void createBarberReturnsCreated() throws Exception {
		when(createBarberUseCase.create(any(CreateBarberCommand.class))).thenReturn(barber(true));

		mockMvc.perform(post("/api/v1/barbers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Carlos Gomez",
								  "phone": "3101234567",
								  "email": "carlos@example.com"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/barbers/1"))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void rejectInvalidCreateBarberRequest() throws Exception {
		mockMvc.perform(post("/api/v1/barbers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "",
								  "phone": "",
								  "email": "invalid-email"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request"));

		verify(createBarberUseCase, never()).create(any(CreateBarberCommand.class));
	}

	@Test
	void listBarbersReturnsOk() throws Exception {
		when(listBarbersUseCase.list()).thenReturn(List.of(barber(true)));

		mockMvc.perform(get("/api/v1/barbers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].phone").value("3101234567"));
	}

	@Test
	void getBarberReturnsOk() throws Exception {
		when(getBarberUseCase.getById(1L)).thenReturn(barber(true));

		mockMvc.perform(get("/api/v1/barbers/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.fullName").value("Carlos Gomez"));
	}

	@Test
	void getMissingBarberReturnsNotFound() throws Exception {
		when(getBarberUseCase.getById(99L)).thenThrow(new BarberNotFoundException("Barber not found"));

		mockMvc.perform(get("/api/v1/barbers/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Barber not found"));
	}

	@Test
	void updateBarberReturnsOk() throws Exception {
		when(updateBarberUseCase.update(any(Long.class), any(UpdateBarberCommand.class)))
				.thenReturn(barber(true));

		mockMvc.perform(put("/api/v1/barbers/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Carlos Gomez",
								  "phone": "3101234567",
								  "email": "carlos@example.com"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void activateBarberReturnsOk() throws Exception {
		when(activateBarberUseCase.activate(1L)).thenReturn(barber(true));

		mockMvc.perform(patch("/api/v1/barbers/1/activate"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void deactivateBarberReturnsOk() throws Exception {
		when(deactivateBarberUseCase.deactivate(1L)).thenReturn(barber(false));

		mockMvc.perform(patch("/api/v1/barbers/1/deactivate"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
	}

	private BarberResponse barber(boolean active) {
		return new BarberResponse(
				1L,
				"Carlos Gomez",
				"3101234567",
				"carlos@example.com",
				active,
				CREATED_AT,
				UPDATED_AT
		);
	}
}
