package com.villamil.barberbooking.infrastructure.adapter.in.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.villamil.barberbooking.application.dto.response.CompanyCapabilitiesResponse;
import com.villamil.barberbooking.application.port.in.CompanyCapabilitiesUseCase;
import com.villamil.barberbooking.domain.capabilities.CapabilityTier;

class CompanyCapabilitiesControllerTest {
    @Test void returnsOnlyCommercialRightsWithoutTenantOrAccountIdentifiers() throws Exception {
        var useCase = mock(CompanyCapabilitiesUseCase.class);
        when(useCase.currentCapabilities()).thenReturn(
                new CompanyCapabilitiesResponse(CapabilityTier.FREE, true, false, true));
        var mvc = MockMvcBuilders.standaloneSetup(new CompanyCapabilitiesController(useCase)).build();
        mvc.perform(get("/api/v1/company/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("FREE"))
                .andExpect(jsonPath("$.basicBooking").value(true))
                .andExpect(jsonPath("$.teamManagement").value(false))
                .andExpect(jsonPath("$.companyActive").value(true))
                .andExpect(jsonPath("$.companyId").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
        verify(useCase).currentCapabilities();
    }
}
