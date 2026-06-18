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

import java.math.BigDecimal;
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
import com.villamil.barberbooking.application.dto.command.CreateServiceOfferingCommand;
import com.villamil.barberbooking.application.dto.command.UpdateServiceOfferingCommand;
import com.villamil.barberbooking.application.dto.response.ServiceOfferingResponse;
import com.villamil.barberbooking.application.port.in.ActivateServiceOfferingUseCase;
import com.villamil.barberbooking.application.port.in.CreateServiceOfferingUseCase;
import com.villamil.barberbooking.application.port.in.DeactivateServiceOfferingUseCase;
import com.villamil.barberbooking.application.port.in.GetServiceOfferingUseCase;
import com.villamil.barberbooking.application.port.in.ListServiceOfferingsUseCase;
import com.villamil.barberbooking.application.port.in.UpdateServiceOfferingUseCase;
import com.villamil.barberbooking.domain.exception.ServiceOfferingNotFoundException;

@ExtendWith(MockitoExtension.class)
class ServiceOfferingControllerTest {

	private static final Instant CREATED_AT = Instant.parse("2026-06-17T12:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-06-17T12:30:00Z");

	@Mock
	private CreateServiceOfferingUseCase createServiceOfferingUseCase;

	@Mock
	private GetServiceOfferingUseCase getServiceOfferingUseCase;

	@Mock
	private ListServiceOfferingsUseCase listServiceOfferingsUseCase;

	@Mock
	private UpdateServiceOfferingUseCase updateServiceOfferingUseCase;

	@Mock
	private ActivateServiceOfferingUseCase activateServiceOfferingUseCase;

	@Mock
	private DeactivateServiceOfferingUseCase deactivateServiceOfferingUseCase;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper().findAndRegisterModules();
		ServiceOfferingController controller = new ServiceOfferingController(
				createServiceOfferingUseCase,
				getServiceOfferingUseCase,
				listServiceOfferingsUseCase,
				updateServiceOfferingUseCase,
				activateServiceOfferingUseCase,
				deactivateServiceOfferingUseCase
		);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	void createServiceOfferingReturnsCreated() throws Exception {
		when(createServiceOfferingUseCase.create(any(CreateServiceOfferingCommand.class))).thenReturn(serviceOffering(true));

		mockMvc.perform(post("/api/v1/services")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Corte clasico",
								  "description": "Corte tradicional",
								  "durationMinutes": 30,
								  "price": 25000.00
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/services/1"))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void rejectInvalidCreateServiceOfferingRequest() throws Exception {
		mockMvc.perform(post("/api/v1/services")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "",
								  "durationMinutes": 0,
								  "price": -1
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request"));

		verify(createServiceOfferingUseCase, never()).create(any(CreateServiceOfferingCommand.class));
	}

	@Test
	void listServiceOfferingsReturnsOk() throws Exception {
		when(listServiceOfferingsUseCase.list()).thenReturn(List.of(serviceOffering(true)));

		mockMvc.perform(get("/api/v1/services"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name").value("Corte clasico"));
	}

	@Test
	void getServiceOfferingReturnsOk() throws Exception {
		when(getServiceOfferingUseCase.getById(1L)).thenReturn(serviceOffering(true));

		mockMvc.perform(get("/api/v1/services/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.durationMinutes").value(30));
	}

	@Test
	void getMissingServiceOfferingReturnsNotFound() throws Exception {
		when(getServiceOfferingUseCase.getById(99L))
				.thenThrow(new ServiceOfferingNotFoundException("Service offering not found"));

		mockMvc.perform(get("/api/v1/services/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Service offering not found"));
	}

	@Test
	void updateServiceOfferingReturnsOk() throws Exception {
		when(updateServiceOfferingUseCase.update(any(Long.class), any(UpdateServiceOfferingCommand.class)))
				.thenReturn(serviceOffering(true));

		mockMvc.perform(put("/api/v1/services/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Corte clasico",
								  "description": "Corte tradicional",
								  "durationMinutes": 30,
								  "price": 25000.00
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void activateServiceOfferingReturnsOk() throws Exception {
		when(activateServiceOfferingUseCase.activate(1L)).thenReturn(serviceOffering(true));

		mockMvc.perform(patch("/api/v1/services/1/activate"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void deactivateServiceOfferingReturnsOk() throws Exception {
		when(deactivateServiceOfferingUseCase.deactivate(1L)).thenReturn(serviceOffering(false));

		mockMvc.perform(patch("/api/v1/services/1/deactivate"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
	}

	private ServiceOfferingResponse serviceOffering(boolean active) {
		return new ServiceOfferingResponse(
				1L,
				"Corte clasico",
				"Corte tradicional",
				30,
				new BigDecimal("25000.00"),
				active,
				CREATED_AT,
				UPDATED_AT
		);
	}
}
