package com.villamil.barberbooking.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.villamil.barberbooking.application.dto.response.CompanyPublicBrandingResponse;
import com.villamil.barberbooking.application.port.in.GetCurrentCompanyPublicBrandingUseCase;
import com.villamil.barberbooking.application.port.in.UpdateCompanyPublicBrandingUseCase;
import com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request.UpdateCompanyPublicBrandingRequest;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/companies/public-profile")
public class CompanyPublicBrandingController {

	private final GetCurrentCompanyPublicBrandingUseCase getCurrentCompanyPublicBrandingUseCase;
	private final UpdateCompanyPublicBrandingUseCase updateCompanyPublicBrandingUseCase;

	public CompanyPublicBrandingController(
			GetCurrentCompanyPublicBrandingUseCase getCurrentCompanyPublicBrandingUseCase,
			UpdateCompanyPublicBrandingUseCase updateCompanyPublicBrandingUseCase
	) {
		this.getCurrentCompanyPublicBrandingUseCase = getCurrentCompanyPublicBrandingUseCase;
		this.updateCompanyPublicBrandingUseCase = updateCompanyPublicBrandingUseCase;
	}

	@GetMapping
	public ResponseEntity<CompanyPublicBrandingResponse> getCurrent() {
		return ResponseEntity.ok(getCurrentCompanyPublicBrandingUseCase.getCurrent());
	}

	@PutMapping
	public ResponseEntity<CompanyPublicBrandingResponse> update(
			@Valid @RequestBody UpdateCompanyPublicBrandingRequest request
	) {
		return ResponseEntity.ok(updateCompanyPublicBrandingUseCase.update(request.toCommand()));
	}
}
