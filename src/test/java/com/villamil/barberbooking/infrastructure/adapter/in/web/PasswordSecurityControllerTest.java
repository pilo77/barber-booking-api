package com.villamil.barberbooking.infrastructure.adapter.in.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.villamil.barberbooking.application.dto.command.ChangePasswordCommand;
import com.villamil.barberbooking.application.port.in.PasswordSecurityUseCase;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;

class PasswordSecurityControllerTest {
    private PasswordSecurityUseCase useCase;
    private MockMvc mvc;

    @BeforeEach void setUp() {
        useCase = mock(PasswordSecurityUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new PasswordSecurityController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test void successfulChangeReturnsNoTokenOrPasswordBody() throws Exception {
        mvc.perform(post("/api/v1/auth/change-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"qa-existing-password\",\"newPassword\":\"qa-new-password-123!\"}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(useCase).changePassword(new ChangePasswordCommand("qa-existing-password", "qa-new-password-123!"));
    }

    @Test void invalidCurrentPasswordIsBadRequestAndDoesNotReturnCredentialValues() throws Exception {
        doThrow(new BusinessRuleException("Unable to change password with the supplied data"))
                .when(useCase).changePassword(any());
        mvc.perform(post("/api/v1/auth/change-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"wrong-current-value\",\"newPassword\":\"qa-new-password-123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unable to change password with the supplied data"));
    }

    @Test void shortReplacementIsRejectedBeforeUseCase() throws Exception {
        mvc.perform(post("/api/v1/auth/change-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"qa-existing-password\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Password change data is invalid"));
        verifyNoInteractions(useCase);
    }

    @Test void requestToStringIsRedacted() {
        var request = new PasswordSecurityController.ChangePasswordRequest("private-old", "private-new");
        org.junit.jupiter.api.Assertions.assertEquals("ChangePasswordRequest[redacted]", request.toString());
    }
}
