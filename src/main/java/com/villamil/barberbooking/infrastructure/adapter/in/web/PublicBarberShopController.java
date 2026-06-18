package com.villamil.barberbooking.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.villamil.barberbooking.application.dto.response.PublicBarberResponse;
import com.villamil.barberbooking.application.dto.response.PublicBarberShopResponse;
import com.villamil.barberbooking.application.dto.response.PublicBranchResponse;
import com.villamil.barberbooking.application.dto.response.PublicServiceOfferingResponse;
import com.villamil.barberbooking.application.port.in.GetPublicBarberShopUseCase;
import com.villamil.barberbooking.application.port.in.GetPublicBranchUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicBarbersUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicBranchesUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicServicesUseCase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/api/v1/public/barber-shops")
public class PublicBarberShopController {

	private final GetPublicBarberShopUseCase getPublicBarberShopUseCase;
	private final ListPublicBranchesUseCase listPublicBranchesUseCase;
	private final GetPublicBranchUseCase getPublicBranchUseCase;
	private final ListPublicServicesUseCase listPublicServicesUseCase;
	private final ListPublicBarbersUseCase listPublicBarbersUseCase;

	public PublicBarberShopController(
			GetPublicBarberShopUseCase getPublicBarberShopUseCase,
			ListPublicBranchesUseCase listPublicBranchesUseCase,
			GetPublicBranchUseCase getPublicBranchUseCase,
			ListPublicServicesUseCase listPublicServicesUseCase,
			ListPublicBarbersUseCase listPublicBarbersUseCase
	) {
		this.getPublicBarberShopUseCase = getPublicBarberShopUseCase;
		this.listPublicBranchesUseCase = listPublicBranchesUseCase;
		this.getPublicBranchUseCase = getPublicBranchUseCase;
		this.listPublicServicesUseCase = listPublicServicesUseCase;
		this.listPublicBarbersUseCase = listPublicBarbersUseCase;
	}

	@GetMapping("/{companySlug}")
	public ResponseEntity<PublicBarberShopResponse> getBarberShop(
			@PathVariable @NotBlank @Size(max = 120) String companySlug
	) {
		return ResponseEntity.ok(getPublicBarberShopUseCase.getBySlug(companySlug));
	}

	@GetMapping("/{companySlug}/branches")
	public ResponseEntity<List<PublicBranchResponse>> listBranches(
			@PathVariable @NotBlank @Size(max = 120) String companySlug
	) {
		return ResponseEntity.ok(listPublicBranchesUseCase.listByCompanySlug(companySlug));
	}

	@GetMapping("/{companySlug}/branches/{branchSlug}")
	public ResponseEntity<PublicBranchResponse> getBranch(
			@PathVariable @NotBlank @Size(max = 120) String companySlug,
			@PathVariable @NotBlank @Size(max = 120) String branchSlug
	) {
		return ResponseEntity.ok(getPublicBranchUseCase.getBySlug(companySlug, branchSlug));
	}

	@GetMapping("/{companySlug}/branches/{branchSlug}/services")
	public ResponseEntity<List<PublicServiceOfferingResponse>> listServices(
			@PathVariable @NotBlank @Size(max = 120) String companySlug,
			@PathVariable @NotBlank @Size(max = 120) String branchSlug
	) {
		return ResponseEntity.ok(listPublicServicesUseCase.listServicesByBranchSlug(companySlug, branchSlug));
	}

	@GetMapping("/{companySlug}/branches/{branchSlug}/barbers")
	public ResponseEntity<List<PublicBarberResponse>> listBarbers(
			@PathVariable @NotBlank @Size(max = 120) String companySlug,
			@PathVariable @NotBlank @Size(max = 120) String branchSlug
	) {
		return ResponseEntity.ok(listPublicBarbersUseCase.listBarbersByBranchSlug(companySlug, branchSlug));
	}
}
