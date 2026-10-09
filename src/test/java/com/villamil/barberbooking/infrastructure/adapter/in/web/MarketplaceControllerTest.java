package com.villamil.barberbooking.infrastructure.adapter.in.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villamil.barberbooking.application.dto.response.*;
import com.villamil.barberbooking.application.port.in.MarketplaceUseCase;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;
import com.villamil.barberbooking.domain.model.MarketplaceBranchProfile;

class MarketplaceControllerTest {
    MarketplaceUseCase marketplace = mock(MarketplaceUseCase.class);
    MockMvc mvc;
    @BeforeEach void controller() {
        mvc = MockMvcBuilders.standaloneSetup(new MarketplaceController(marketplace))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper().findAndRegisterModules())).build();
    }

    @Test void catalogContainsOnlyPublicFieldsAndPagination() throws Exception {
        var item = new PublicMarketplaceItemResponse("alpha-shop", "main", "Alpha shop", "Main", "Neiva", "Centro",
                "Public address", "Reviewed description", "3001234567", "https://cdn.example.test/cover.jpg", new BigDecimal("20000"));
        when(marketplace.search("Neiva", "Corte", "Alpha", 0, 12)).thenReturn(new PublicMarketplacePageResponse(List.of(item), 0, 12, false));
        mvc.perform(get("/api/v1/public/marketplace").param("city", "Neiva").param("service", "Corte").param("q", "Alpha"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].companySlug").value("alpha-shop"))
                .andExpect(jsonPath("$.items[0].startingPrice").value(20000))
                .andExpect(jsonPath("$.items[0].companyId").doesNotExist()).andExpect(jsonPath("$.items[0].branchId").doesNotExist())
                .andExpect(jsonPath("$.items[0].email").doesNotExist()).andExpect(jsonPath("$.items[0].publicationState").doesNotExist())
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(12)).andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test void oversizedProfileIsRejectedBeforeUseCase() throws Exception {
        mvc.perform(put("/api/v1/company/marketplace-profile").contentType("application/json")
                .content("{\"city\":\"" + "x".repeat(121) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(marketplace);
    }

    @Test void ownProfileDoesNotExposeReviewerOrTenantIdentifiers() throws Exception {
        when(marketplace.currentProfile()).thenReturn(MarketplaceProfileResponse.from(MarketplaceBranchProfile.draft(7L, 9L)));
        mvc.perform(get("/api/v1/company/marketplace-profile")).andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationState").value("DRAFT"))
                .andExpect(jsonPath("$.companyId").doesNotExist()).andExpect(jsonPath("$.branchId").doesNotExist())
                .andExpect(jsonPath("$.reviewedBy").doesNotExist());
    }

    @Test void businessRuleAndPermissionDenialsUseControlledResponses() throws Exception {
        when(marketplace.submit()).thenThrow(new BusinessRuleException("Publication needs complete public information"));
        mvc.perform(post("/api/v1/company/marketplace-profile/submit")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Publication needs complete public information"));
        when(marketplace.pending()).thenThrow(new ForbiddenOperationException("Only the platform owner may review publication"));
        mvc.perform(get("/api/v1/platform/marketplace/submissions")).andExpect(status().isForbidden());
    }

    @Test void invalidPaginationTypeAndOversizedReviewAreRejected() throws Exception {
        mvc.perform(get("/api/v1/public/marketplace").param("page", "not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/platform/marketplace/submissions/9/reject").contentType("application/json")
                .content("{\"reason\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(marketplace);
    }

    @Test void moderatorMustSendTheReviewedProfileVersion() throws Exception {
        mvc.perform(patch("/api/v1/platform/marketplace/submissions/9/approve").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(marketplace);
    }
}
