package com.villamil.barberbooking.infrastructure.adapter.in.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.villamil.barberbooking.application.dto.response.PublicBarberResponse;
import com.villamil.barberbooking.application.dto.response.PublicBarberShopResponse;
import com.villamil.barberbooking.application.dto.response.PublicBranchResponse;
import com.villamil.barberbooking.application.dto.response.PublicServiceOfferingResponse;
import com.villamil.barberbooking.application.port.in.GetPublicBarberShopUseCase;
import com.villamil.barberbooking.application.port.in.GetPublicBranchUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicBarbersUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicBranchesUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicServicesUseCase;
import com.villamil.barberbooking.domain.exception.PublicResourceNotFoundException;

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

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		PublicBarberShopController controller = new PublicBarberShopController(
				getPublicBarberShopUseCase,
				listPublicBranchesUseCase,
				getPublicBranchUseCase,
				listPublicServicesUseCase,
				listPublicBarbersUseCase
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
}
