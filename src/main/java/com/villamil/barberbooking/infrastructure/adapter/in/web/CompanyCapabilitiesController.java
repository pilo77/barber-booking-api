package com.villamil.barberbooking.infrastructure.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.villamil.barberbooking.application.dto.response.CompanyCapabilitiesResponse;
import com.villamil.barberbooking.application.port.in.CompanyCapabilitiesUseCase;

@RestController
public class CompanyCapabilitiesController {
    private final CompanyCapabilitiesUseCase capabilities;

    public CompanyCapabilitiesController(CompanyCapabilitiesUseCase capabilities) {
        this.capabilities = capabilities;
    }

    @GetMapping("/api/v1/company/capabilities")
    public CompanyCapabilitiesResponse current() {
        return capabilities.currentCapabilities();
    }
}
