package com.villamil.barberbooking.infrastructure.config;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.villamil.barberbooking.application.dto.response.CustomerResponse;
import com.villamil.barberbooking.application.port.in.CreateCustomerUseCase;
import com.villamil.barberbooking.application.port.in.DeactivateCustomerUseCase;
import com.villamil.barberbooking.application.port.in.GetCustomerUseCase;
import com.villamil.barberbooking.application.port.in.ListCustomersUseCase;
import com.villamil.barberbooking.application.port.in.UpdateCustomerUseCase;
import com.villamil.barberbooking.application.port.out.JwtTokenPort;
import com.villamil.barberbooking.infrastructure.adapter.in.web.CustomerController;
import com.villamil.barberbooking.infrastructure.security.JwtAuthenticationFilter;
import com.villamil.barberbooking.infrastructure.tenant.TemporaryTenantHeaderFilter;
import com.villamil.barberbooking.infrastructure.tenant.ThreadLocalTenantContextProvider;

@WebMvcTest(CustomerController.class)
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

	@Test
	void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/customers"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpointWithInsufficientRoleReturnsForbidden() throws Exception {
		mockMvc.perform(get("/api/v1/customers").with(user("customer@example.com").roles("CUSTOMER")))
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
