package com.villamil.barberbooking.infrastructure.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.villamil.barberbooking.application.dto.response.CustomerResponse;
import com.villamil.barberbooking.application.dto.response.BarberAvailabilityResponse;
import com.villamil.barberbooking.application.dto.response.PublicAppointmentResponse;
import com.villamil.barberbooking.application.dto.response.PublicBarberShopResponse;
import com.villamil.barberbooking.application.dto.response.CompanyPublicBrandingResponse;
import com.villamil.barberbooking.application.port.in.CreateCustomerUseCase;
import com.villamil.barberbooking.application.port.in.GetCurrentCompanyPublicBrandingUseCase;
import com.villamil.barberbooking.application.port.in.CreatePublicAppointmentUseCase;
import com.villamil.barberbooking.application.port.in.DeactivateCustomerUseCase;
import com.villamil.barberbooking.application.port.in.GetCustomerUseCase;
import com.villamil.barberbooking.application.port.in.GetPublicBarberAvailabilityUseCase;
import com.villamil.barberbooking.application.port.in.GetPublicBarberShopUseCase;
import com.villamil.barberbooking.application.port.in.GetPublicBranchUseCase;
import com.villamil.barberbooking.application.port.in.ListCustomersUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicBarbersUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicBranchesUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicServicesUseCase;
import com.villamil.barberbooking.application.port.in.UpdateCompanyPublicBrandingUseCase;
import com.villamil.barberbooking.application.port.in.UpdateCustomerUseCase;
import com.villamil.barberbooking.application.port.out.JwtTokenPort;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;
import com.villamil.barberbooking.domain.valueobject.ThemeMode;
import com.villamil.barberbooking.infrastructure.adapter.in.web.CompanyPublicBrandingController;
import com.villamil.barberbooking.infrastructure.adapter.in.web.CustomerController;
import com.villamil.barberbooking.infrastructure.adapter.in.web.PublicBarberShopController;
import com.villamil.barberbooking.infrastructure.security.JwtAuthenticationFilter;
import com.villamil.barberbooking.infrastructure.tenant.TemporaryTenantHeaderFilter;
import com.villamil.barberbooking.infrastructure.tenant.ThreadLocalTenantContextProvider;

@WebMvcTest({CustomerController.class, PublicBarberShopController.class, CompanyPublicBrandingController.class})
@Import({
		SecurityConfig.class,
		JwtAuthenticationFilter.class,
		TemporaryTenantHeaderFilter.class,
		ThreadLocalTenantContextProvider.class
})
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtTokenPort jwtTokenPort;

	@MockitoBean
	private CreateCustomerUseCase createCustomerUseCase;

	@MockitoBean
	private GetCustomerUseCase getCustomerUseCase;

	@MockitoBean
	private ListCustomersUseCase listCustomersUseCase;

	@MockitoBean
	private UpdateCustomerUseCase updateCustomerUseCase;

	@MockitoBean
	private DeactivateCustomerUseCase deactivateCustomerUseCase;

	@MockitoBean
	private GetPublicBarberShopUseCase getPublicBarberShopUseCase;

	@MockitoBean
	private ListPublicBranchesUseCase listPublicBranchesUseCase;

	@MockitoBean
	private GetPublicBranchUseCase getPublicBranchUseCase;

	@MockitoBean
	private ListPublicServicesUseCase listPublicServicesUseCase;

	@MockitoBean
	private ListPublicBarbersUseCase listPublicBarbersUseCase;

	@MockitoBean
	private GetPublicBarberAvailabilityUseCase getPublicBarberAvailabilityUseCase;

	@MockitoBean
	private CreatePublicAppointmentUseCase createPublicAppointmentUseCase;

	@MockitoBean
	private GetCurrentCompanyPublicBrandingUseCase getCurrentCompanyPublicBrandingUseCase;

	@MockitoBean
	private UpdateCompanyPublicBrandingUseCase updateCompanyPublicBrandingUseCase;

	@Test
	void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/customers"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void publicEndpointWithoutTokenReturnsOk() throws Exception {
		when(getPublicBarberShopUseCase.getBySlug("ponte-perro"))
				.thenReturn(new PublicBarberShopResponse(
						"ponte-perro",
						"Ponte Perro",
						null,
						null,
						true,
						new CompanyPublicBrandingResponse(
								"Ponte Perro",
								null,
								null,
								null,
								null,
								null,
								null,
								ThemeMode.SYSTEM,
								null,
								null,
								null,
								null,
								null,
								null
						)
				));

		mockMvc.perform(get("/api/v1/public/barber-shops/ponte-perro"))
				.andExpect(status().isOk());
	}

	@Test
	void publicBookingPostWithoutTokenIsAllowed() throws Exception {
		LocalDateTime startAt = LocalDateTime.of(2026, 6, 20, 10, 0);
		String futureStartAt = LocalDateTime.now().plusDays(1).withNano(0).toString();
		when(createPublicAppointmentUseCase.create(any())).thenReturn(new PublicAppointmentResponse(
				10L,
				AppointmentStatus.SCHEDULED,
				AppointmentSource.ONLINE,
				startAt,
				startAt.plusMinutes(30),
				new PublicAppointmentResponse.ServiceSummary(1L, "Corte clasico", 30, new BigDecimal("25000.00")),
				new PublicAppointmentResponse.BarberSummary(2L, "Santiago", null),
				new PublicAppointmentResponse.CustomerSummary("Carlos", "3001234567")
		));

		mockMvc.perform(post("/api/v1/public/barber-shops/ponte-perro/branches/neiva-centro/appointments")
					.contentType("application/json")
					.header("Idempotency-Key", "booking-key-123")
					.header("X-Company-Id", "not-a-number")
					.header("X-Branch-Id", "999")
					.content("""
							{
							  "serviceOfferingId": 1,
							  "barberId": 2,
							  "startAt": "%s",
							  "customer": {"fullName": "Carlos", "phone": "3001234567"}
							}
							""".formatted(futureStartAt)))
				.andExpect(status().isCreated());
	}

	@Test
	void publicAvailabilityWithoutTokenIsAllowed() throws Exception {
		LocalDate date = LocalDate.now().plusDays(1);
		when(getPublicBarberAvailabilityUseCase.getAvailability(
				"ponte-perro", "neiva-centro", 2L, 1L, date))
				.thenReturn(new BarberAvailabilityResponse(2L, 1L, date, List.of()));

		mockMvc.perform(get("/api/v1/public/barber-shops/ponte-perro/branches/neiva-centro/barbers/2/availability")
					.param("date", date.toString())
					.param("serviceOfferingId", "1"))
				.andExpect(status().isOk());
	}

	@Test
	void unrelatedPublicPostWithoutTokenRemainsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/public/other-action"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void appointmentAdminEndpointWithoutTokenRemainsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/appointments"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void userAccountsEndpointWithoutTokenRemainsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/user-accounts"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void companyBrandingEndpointWithoutTokenRemainsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/companies/public-profile"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void customersEndpointWithoutTokenRemainsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/customers"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void companyOwnerCanGetCompanyBranding() throws Exception {
		when(getCurrentCompanyPublicBrandingUseCase.getCurrent()).thenReturn(new CompanyPublicBrandingResponse(
				"Ponte Perro",
				null,
				null,
				null,
				null,
				null,
				null,
				ThemeMode.SYSTEM,
				null,
				null,
				null,
				null,
				null,
				null
		));

		mockMvc.perform(get("/api/v1/companies/public-profile").with(user("owner@example.com").roles("COMPANY_OWNER")))
				.andExpect(status().isOk());
	}

	@Test
	void companyOwnerCanPutCompanyBranding() throws Exception {
		when(updateCompanyPublicBrandingUseCase.update(any())).thenReturn(new CompanyPublicBrandingResponse(
				"Ponte Perro",
				null,
				"https://cdn.example.com/logo.png",
				null,
				null,
				null,
				null,
				ThemeMode.SYSTEM,
				null,
				null,
				null,
				null,
				null,
				null
		));

		mockMvc.perform(put("/api/v1/companies/public-profile")
					.with(user("owner@example.com").roles("COMPANY_OWNER"))
					.contentType("application/json")
					.content("""
							{
							  "publicName": "Ponte Perro",
							  "themeMode": "SYSTEM"
							}
							"""))
				.andExpect(status().isOk());
	}

	@Test
	void companyOwnerCanListCustomers() throws Exception {
		when(listCustomersUseCase.list()).thenReturn(List.of(customer()));

		mockMvc.perform(get("/api/v1/customers").with(user("owner@example.com").roles("COMPANY_OWNER")))
				.andExpect(status().isOk());
	}

	@Test
	void platformOwnerCannotAccessCompanyBrandingEndpoint() throws Exception {
		mockMvc.perform(get("/api/v1/companies/public-profile").with(user("platform@example.com").roles("PLATFORM_OWNER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void barberCannotAccessCompanyBrandingEndpoint() throws Exception {
		mockMvc.perform(get("/api/v1/companies/public-profile").with(user("barber@example.com").roles("BARBER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void platformOwnerCannotAccessCustomersEndpoint() throws Exception {
		mockMvc.perform(get("/api/v1/customers").with(user("platform@example.com").roles("PLATFORM_OWNER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void platformOwnerCannotAccessBarberAvailabilityEndpoint() throws Exception {
		mockMvc.perform(get("/api/v1/barbers/2/availability").with(user("platform@example.com").roles("PLATFORM_OWNER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void platformOwnerCannotAccessAppointmentsEndpoint() throws Exception {
		mockMvc.perform(get("/api/v1/appointments/10").with(user("platform@example.com").roles("PLATFORM_OWNER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void platformOwnerCannotAccessServicesEndpoint() throws Exception {
		mockMvc.perform(get("/api/v1/services").with(user("platform@example.com").roles("PLATFORM_OWNER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void protectedEndpointWithInsufficientRoleReturnsForbidden() throws Exception {
		mockMvc.perform(get("/api/v1/customers").with(user("customer@example.com").roles("CUSTOMER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void barberCannotListCustomers() throws Exception {
		mockMvc.perform(get("/api/v1/customers").with(user("barber@example.com").roles("BARBER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void protectedEndpointWithAllowedRoleReturnsOk() throws Exception {
		when(listCustomersUseCase.list()).thenReturn(List.of(customer()));

		mockMvc.perform(get("/api/v1/customers").with(user("reception@example.com").roles("RECEPTIONIST")))
				.andExpect(status().isOk());
	}

	private CustomerResponse customer() {
		return new CustomerResponse(
				1L,
				"Ana Perez",
				"3001234567",
				"ana@example.com",
				true,
				Instant.parse("2026-06-18T12:00:00Z"),
				Instant.parse("2026-06-18T12:00:00Z")
		);
	}
}
