package com.villamil.barberbooking.infrastructure.adapter.in.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.villamil.barberbooking.application.dto.response.AvailabilitySlotResponse;
import com.villamil.barberbooking.application.dto.response.BarberAvailabilityResponse;
import com.villamil.barberbooking.application.dto.response.PublicAppointmentResponse;
import com.villamil.barberbooking.application.dto.response.PublicBarberResponse;
import com.villamil.barberbooking.application.dto.response.PublicBarberShopResponse;
import com.villamil.barberbooking.application.dto.response.PublicBranchResponse;
import com.villamil.barberbooking.application.dto.response.PublicServiceOfferingResponse;
import com.villamil.barberbooking.application.port.in.CreatePublicAppointmentUseCase;
import com.villamil.barberbooking.application.port.in.GetPublicBarberAvailabilityUseCase;
import com.villamil.barberbooking.application.port.in.GetPublicBarberShopUseCase;
import com.villamil.barberbooking.application.port.in.GetPublicBranchUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicBarbersUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicBranchesUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicServicesUseCase;
import com.villamil.barberbooking.application.exception.MissingIdempotencyKeyException;
import com.villamil.barberbooking.domain.exception.PublicResourceNotFoundException;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

@ExtendWith(MockitoExtension.class)
class PublicBarberShopControllerTest {

	@Mock
	private GetPublicBarberShopUseCase getPublicBarberShopUseCase;

	@Mock
	private ListPublicBranchesUseCase listPublicBranchesUseCase;

	@Mock
	private GetPublicBranchUseCase getPublicBranchUseCase;

	@Mock
	private ListPublicServicesUseCase listPublicServicesUseCase;

	@Mock
	private ListPublicBarbersUseCase listPublicBarbersUseCase;

	@Mock
	private GetPublicBarberAvailabilityUseCase getPublicBarberAvailabilityUseCase;

	@Mock
	private CreatePublicAppointmentUseCase createPublicAppointmentUseCase;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		PublicBarberShopController controller = new PublicBarberShopController(
				getPublicBarberShopUseCase,
				listPublicBranchesUseCase,
				getPublicBranchUseCase,
				listPublicServicesUseCase,
				listPublicBarbersUseCase,
				getPublicBarberAvailabilityUseCase,
				createPublicAppointmentUseCase
		);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	void getPublicCompanyProfileReturnsOk() throws Exception {
		when(getPublicBarberShopUseCase.getBySlug("ponte-perro"))
				.thenReturn(new PublicBarberShopResponse(
						"ponte-perro",
						"Ponte Perro Barberia",
						"Cortes modernos",
						"https://cdn.example.com/logo.png",
						true
				));

		mockMvc.perform(get("/api/v1/public/barber-shops/ponte-perro"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slug").value("ponte-perro"))
				.andExpect(jsonPath("$.name").value("Ponte Perro Barberia"))
				.andExpect(jsonPath("$.description").value("Cortes modernos"));
	}

	@Test
	void inactiveCompanyReturnsNotFound() throws Exception {
		when(getPublicBarberShopUseCase.getBySlug("inactive-shop"))
				.thenThrow(new PublicResourceNotFoundException("Public barber shop not found"));

		mockMvc.perform(get("/api/v1/public/barber-shops/inactive-shop"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Public resource not found"));
	}

	@Test
	void listBranchesReturnsPublicBranchData() throws Exception {
		when(listPublicBranchesUseCase.listByCompanySlug("ponte-perro"))
				.thenReturn(List.of(new PublicBranchResponse("neiva-centro", "Neiva Centro", "Calle 1", "3001234567")));

		mockMvc.perform(get("/api/v1/public/barber-shops/ponte-perro/branches"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].slug").value("neiva-centro"));
	}

	@Test
	void getBranchUsesCompanyAndBranchSlug() throws Exception {
		when(getPublicBranchUseCase.getBySlug("ponte-perro", "neiva-centro"))
				.thenReturn(new PublicBranchResponse("neiva-centro", "Neiva Centro", "Calle 1", "3001234567"));

		mockMvc.perform(get("/api/v1/public/barber-shops/ponte-perro/branches/neiva-centro"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Neiva Centro"));
	}

	@Test
	void listPublicServicesReturnsOnlyPublicShape() throws Exception {
		when(listPublicServicesUseCase.listServicesByBranchSlug("ponte-perro", "neiva-centro"))
				.thenReturn(List.of(new PublicServiceOfferingResponse(
						1L,
						"Corte clasico",
						"Corte tradicional",
						30,
						new BigDecimal("25000.00")
				)));

		mockMvc.perform(get("/api/v1/public/barber-shops/ponte-perro/branches/neiva-centro/services"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].name").value("Corte clasico"))
				.andExpect(jsonPath("$[0].price").value(25000.00));
	}

	@Test
	void listPublicBarbersDoesNotExposeSensitiveFields() throws Exception {
		when(listPublicBarbersUseCase.listBarbersByBranchSlug("ponte-perro", "neiva-centro"))
				.thenReturn(List.of(new PublicBarberResponse(
						1L,
						"Santiago",
						"https://cdn.example.com/santiago.png",
						"Especialista en fade",
						"Fade, barba"
				)));

		mockMvc.perform(get("/api/v1/public/barber-shops/ponte-perro/branches/neiva-centro/barbers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].displayName").value("Santiago"))
				.andExpect(jsonPath("$[0].email").doesNotExist())
				.andExpect(jsonPath("$[0].phone").doesNotExist());
	}

	@Test
	void getPublicAvailabilityReturnsSlots() throws Exception {
		LocalDate date = LocalDate.now().plusDays(1);
		when(getPublicBarberAvailabilityUseCase.getAvailability("ponte-perro", "neiva-centro", 2L, 1L, date))
				.thenReturn(new BarberAvailabilityResponse(
						2L,
						1L,
						date,
						List.of(AvailabilitySlotResponse.available(
								LocalDateTime.of(2026, 6, 20, 10, 0),
								LocalDateTime.of(2026, 6, 20, 10, 30)
						))
				));

		mockMvc.perform(get("/api/v1/public/barber-shops/ponte-perro/branches/neiva-centro/barbers/2/availability")
					.param("date", date.toString())
					.param("serviceOfferingId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots[0].available").value(true));
	}

	@Test
	void createPublicAppointmentReturnsCreatedWithoutSensitiveFields() throws Exception {
		LocalDateTime startAt = LocalDateTime.of(2026, 6, 20, 10, 0);
		String futureStartAt = LocalDateTime.now().plusDays(1).withNano(0).toString();
		when(createPublicAppointmentUseCase.create(org.mockito.ArgumentMatchers.any()))
				.thenReturn(new PublicAppointmentResponse(
						10L,
						AppointmentStatus.SCHEDULED,
						AppointmentSource.ONLINE,
						startAt,
						startAt.plusMinutes(30),
						new PublicAppointmentResponse.ServiceSummary(1L, "Corte clasico", 30, new BigDecimal("25000.00")),
						new PublicAppointmentResponse.BarberSummary(2L, "Santiago", null),
						new PublicAppointmentResponse.CustomerSummary("Carlos Villamil", "3001234567")
				));

		mockMvc.perform(post("/api/v1/public/barber-shops/ponte-perro/branches/neiva-centro/appointments")
					.contentType("application/json")
					.header("Idempotency-Key", "booking-key-123")
					.content("""
							{
							  "serviceOfferingId": 1,
							  "barberId": 2,
							  "startAt": "%s",
							  "customer": {
							    "fullName": "Carlos Villamil",
							    "phone": "3001234567",
							    "email": "cliente@example.com"
							  }
							}
							""".formatted(futureStartAt)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.source").value("ONLINE"))
				.andExpect(jsonPath("$.status").value("SCHEDULED"))
				.andExpect(jsonPath("$.customer.phone").value("3001234567"))
				.andExpect(jsonPath("$.customer.email").doesNotExist())
				.andExpect(jsonPath("$.customerId").doesNotExist());
	}

	@Test
	void createPublicAppointmentWithoutIdempotencyKeyReturnsBadRequest() throws Exception {
		String futureStartAt = LocalDateTime.now().plusDays(1).withNano(0).toString();
		when(createPublicAppointmentUseCase.create(org.mockito.ArgumentMatchers.any()))
				.thenThrow(new MissingIdempotencyKeyException("Idempotency-Key header is required"));

		mockMvc.perform(post("/api/v1/public/barber-shops/ponte-perro/branches/neiva-centro/appointments")
					.contentType("application/json")
					.content("""
							{
							  "serviceOfferingId": 1,
							  "barberId": 2,
							  "startAt": "%s",
							  "customer": {"fullName": "Carlos", "phone": "3001234567"}
							}
							""".formatted(futureStartAt)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid idempotency key"))
				.andExpect(jsonPath("$.detail").value("Idempotency-Key header is required"));
	}

	@Test
	void publicAppointmentRejectsTenantAndCustomerIdsFromBody() throws Exception {
		String futureStartAt = LocalDateTime.now().plusDays(1).withNano(0).toString();
		mockMvc.perform(post("/api/v1/public/barber-shops/ponte-perro/branches/neiva-centro/appointments")
					.contentType("application/json")
					.header("Idempotency-Key", "booking-key-123")
					.content("""
							{
							  "companyId": 99,
							  "branchId": 99,
							  "customerId": 99,
							  "endAt": "%s",
							  "serviceOfferingId": 1,
							  "barberId": 2,
							  "startAt": "%s",
							  "customer": {"fullName": "Carlos", "phone": "3001234567"}
							}
							""".formatted(futureStartAt, futureStartAt)))
				.andExpect(status().isBadRequest());
	}
}
