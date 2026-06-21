package com.villamil.barberbooking.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.villamil.barberbooking.application.dto.response.CompanyPublicBrandingResponse;
import com.villamil.barberbooking.application.port.in.GetCurrentCompanyPublicBrandingUseCase;
import com.villamil.barberbooking.application.port.in.UpdateCompanyPublicBrandingUseCase;
import com.villamil.barberbooking.domain.valueobject.ThemeMode;

@ExtendWith(MockitoExtension.class)
class CompanyPublicBrandingControllerTest {

	@Mock
	private GetCurrentCompanyPublicBrandingUseCase getCurrentCompanyPublicBrandingUseCase;

	@Mock
	private UpdateCompanyPublicBrandingUseCase updateCompanyPublicBrandingUseCase;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		CompanyPublicBrandingController controller = new CompanyPublicBrandingController(
				getCurrentCompanyPublicBrandingUseCase,
				updateCompanyPublicBrandingUseCase
		);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	void getCurrentReturnsOk() throws Exception {
		when(getCurrentCompanyPublicBrandingUseCase.getCurrent()).thenReturn(response());

		mockMvc.perform(get("/api/v1/companies/public-profile"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.publicName").value("Ponte Perro"))
				.andExpect(jsonPath("$.themeMode").value("SYSTEM"));
	}

	@Test
	void updateWithInvalidPayloadReturnsBadRequest() throws Exception {
		mockMvc.perform(put("/api/v1/companies/public-profile")
					.contentType("application/json")
					.content("""
							{
							  "publicName": "Nombre",
							  "themeMode": "LIGHT",
							  "logoUrl": "%s"
							}
							""".formatted("x".repeat(501))))
				.andExpect(status().isBadRequest());

		verify(updateCompanyPublicBrandingUseCase, never()).update(any());
	}

	@Test
	void updateReturnsOk() throws Exception {
		when(updateCompanyPublicBrandingUseCase.update(any())).thenReturn(response());

		mockMvc.perform(put("/api/v1/companies/public-profile")
					.contentType("application/json")
					.content("""
							{
							  "publicName": "Ponte Perro",
							  "publicDescription": "Cortes modernos",
							  "logoUrl": "https://cdn.example.com/logo.png",
							  "primaryColor": "#111111",
							  "accentColor": "#D4AF37",
							  "themeMode": "SYSTEM"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.logoUrl").value("https://cdn.example.com/logo.png"));
	}

	private CompanyPublicBrandingResponse response() {
		return new CompanyPublicBrandingResponse(
				"Ponte Perro",
				"Cortes modernos",
				"https://cdn.example.com/logo.png",
				null,
				"#111111",
				null,
				"#D4AF37",
				ThemeMode.SYSTEM,
				null,
				null,
				null,
				null,
				null,
				null
		);
	}
}
