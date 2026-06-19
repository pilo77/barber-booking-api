package com.villamil.barberbooking.infrastructure.adapter.in.web;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.villamil.barberbooking.application.dto.response.BarberAvailabilityResponse;
import com.villamil.barberbooking.application.dto.response.PublicAppointmentResponse;
import com.villamil.barberbooking.application.dto.response.PublicBarberResponse;
import com.villamil.barberbooking.application.dto.response.PublicBarberShopResponse;
import com.villamil.barberbooking.application.dto.response.PublicBranchResponse;
import com.villamil.barberbooking.application.dto.response.PublicServiceOfferingResponse;
import com.villamil.barberbooking.application.port.in.CreatePublicAppointmentUseCase;
import com.villamil.barberbooking.application.port.in.GetPublicBarberAvailabilityUseCase;
import com.villamil.barberbooking.application.port.in.GetPublicBarberShopUseCase;
import com.villamil.barberbooking.application.port.in.GetPublicBranchUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicBarbersUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicBranchesUseCase;
import com.villamil.barberbooking.application.port.in.ListPublicServicesUseCase;
import com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request.CreatePublicAppointmentRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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
	private final GetPublicBarberAvailabilityUseCase getPublicBarberAvailabilityUseCase;
	private final CreatePublicAppointmentUseCase createPublicAppointmentUseCase;

	public PublicBarberShopController(
			GetPublicBarberShopUseCase getPublicBarberShopUseCase,
			ListPublicBranchesUseCase listPublicBranchesUseCase,
			GetPublicBranchUseCase getPublicBranchUseCase,
			ListPublicServicesUseCase listPublicServicesUseCase,
			ListPublicBarbersUseCase listPublicBarbersUseCase,
			GetPublicBarberAvailabilityUseCase getPublicBarberAvailabilityUseCase,
			CreatePublicAppointmentUseCase createPublicAppointmentUseCase
	) {
		this.getPublicBarberShopUseCase = getPublicBarberShopUseCase;
		this.listPublicBranchesUseCase = listPublicBranchesUseCase;
		this.getPublicBranchUseCase = getPublicBranchUseCase;
		this.listPublicServicesUseCase = listPublicServicesUseCase;
		this.listPublicBarbersUseCase = listPublicBarbersUseCase;
		this.getPublicBarberAvailabilityUseCase = getPublicBarberAvailabilityUseCase;
		this.createPublicAppointmentUseCase = createPublicAppointmentUseCase;
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

	@GetMapping("/{companySlug}/branches/{branchSlug}/barbers/{barberId}/availability")
	public ResponseEntity<BarberAvailabilityResponse> getAvailability(
			@PathVariable @NotBlank @Size(max = 120) String companySlug,
			@PathVariable @NotBlank @Size(max = 120) String branchSlug,
			@PathVariable @Positive Long barberId,
			@RequestParam @FutureOrPresent @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam @Positive Long serviceOfferingId
	) {
		return ResponseEntity.ok(getPublicBarberAvailabilityUseCase.getAvailability(
				companySlug,
				branchSlug,
				barberId,
				serviceOfferingId,
				date
		));
	}

	@PostMapping("/{companySlug}/branches/{branchSlug}/appointments")
	public ResponseEntity<PublicAppointmentResponse> createAppointment(
			@PathVariable @NotBlank @Size(max = 120) String companySlug,
			@PathVariable @NotBlank @Size(max = 120) String branchSlug,
			@Valid @RequestBody CreatePublicAppointmentRequest request
	) {
		PublicAppointmentResponse response = createPublicAppointmentUseCase.create(
				request.toCommand(companySlug, branchSlug)
		);
		return ResponseEntity
				.created(URI.create("/api/v1/public/barber-shops/" + companySlug
						+ "/branches/" + branchSlug + "/appointments/" + response.id()))
				.body(response);
	}
}
