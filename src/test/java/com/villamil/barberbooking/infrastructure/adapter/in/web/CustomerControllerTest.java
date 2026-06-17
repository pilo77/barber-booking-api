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
import com.villamil.barberbooking.application.dto.command.CreateCustomerCommand;
import com.villamil.barberbooking.application.dto.command.UpdateCustomerCommand;
import com.villamil.barberbooking.application.dto.response.CustomerResponse;
import com.villamil.barberbooking.application.port.in.CreateCustomerUseCase;
import com.villamil.barberbooking.application.port.in.DeactivateCustomerUseCase;
import com.villamil.barberbooking.application.port.in.GetCustomerUseCase;
import com.villamil.barberbooking.application.port.in.ListCustomersUseCase;
import com.villamil.barberbooking.application.port.in.UpdateCustomerUseCase;
import com.villamil.barberbooking.domain.exception.CustomerNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

	private static final Instant CREATED_AT = Instant.parse("2026-06-17T12:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-06-17T12:30:00Z");

	@Mock
	private CreateCustomerUseCase createCustomerUseCase;

	@Mock
	private GetCustomerUseCase getCustomerUseCase;

	@Mock
	private ListCustomersUseCase listCustomersUseCase;

	@Mock
	private UpdateCustomerUseCase updateCustomerUseCase;

	@Mock
	private DeactivateCustomerUseCase deactivateCustomerUseCase;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper().findAndRegisterModules();
		CustomerController controller = new CustomerController(
				createCustomerUseCase,
				getCustomerUseCase,
				listCustomersUseCase,
				updateCustomerUseCase,
				deactivateCustomerUseCase
		);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	void createCustomerReturnsCreated() throws Exception {
		when(createCustomerUseCase.create(any(CreateCustomerCommand.class))).thenReturn(customer());

		mockMvc.perform(post("/api/v1/customers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Ana Perez",
								  "phone": "3001234567",
								  "email": "ana@example.com"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/customers/1"))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void rejectInvalidCreateCustomerRequest() throws Exception {
		mockMvc.perform(post("/api/v1/customers")
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

		verify(createCustomerUseCase, never()).create(any(CreateCustomerCommand.class));
	}

	@Test
	void listCustomersReturnsOk() throws Exception {
		when(listCustomersUseCase.list()).thenReturn(List.of(customer()));

		mockMvc.perform(get("/api/v1/customers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].phone").value("3001234567"));
	}

	@Test
	void getCustomerReturnsOk() throws Exception {
		when(getCustomerUseCase.getById(1L)).thenReturn(customer());

		mockMvc.perform(get("/api/v1/customers/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.fullName").value("Ana Perez"));
	}

	@Test
	void getMissingCustomerReturnsNotFound() throws Exception {
		when(getCustomerUseCase.getById(99L)).thenThrow(new CustomerNotFoundException("Customer not found"));

		mockMvc.perform(get("/api/v1/customers/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Customer not found"));
	}

	@Test
	void updateCustomerReturnsOk() throws Exception {
		when(updateCustomerUseCase.update(any(Long.class), any(UpdateCustomerCommand.class)))
				.thenReturn(customer());

		mockMvc.perform(put("/api/v1/customers/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Ana Perez",
								  "phone": "3001234567",
								  "email": "ana@example.com"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void deactivateCustomerReturnsOk() throws Exception {
		when(deactivateCustomerUseCase.deactivate(1L)).thenReturn(deactivatedCustomer());

		mockMvc.perform(patch("/api/v1/customers/1/deactivate"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
	}

	private CustomerResponse customer() {
		return new CustomerResponse(
				1L,
				"Ana Perez",
				"3001234567",
				"ana@example.com",
				true,
				CREATED_AT,
				UPDATED_AT
		);
	}

	private CustomerResponse deactivatedCustomer() {
		return new CustomerResponse(
				1L,
				"Ana Perez",
				"3001234567",
				"ana@example.com",
				false,
				CREATED_AT,
				UPDATED_AT
		);
	}
}
