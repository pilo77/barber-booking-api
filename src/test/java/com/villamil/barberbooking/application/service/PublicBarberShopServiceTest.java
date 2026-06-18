package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.villamil.barberbooking.application.dto.response.PublicBarberResponse;
import com.villamil.barberbooking.application.dto.response.PublicBarberShopResponse;
import com.villamil.barberbooking.application.dto.response.PublicBranchResponse;
import com.villamil.barberbooking.application.dto.response.PublicServiceOfferingResponse;
import com.villamil.barberbooking.application.port.out.PublicBarberShopRepositoryPort;
import com.villamil.barberbooking.domain.exception.PublicResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class PublicBarberShopServiceTest {

	@Mock
	private PublicBarberShopRepositoryPort publicBarberShopRepositoryPort;

	@Test
	void activeCompanyProfileBySlugReturnsPublicData() {
		PublicBarberShopService service = new PublicBarberShopService(publicBarberShopRepositoryPort);
		PublicBarberShopResponse profile = new PublicBarberShopResponse(
				"ponte-perro",
				"Ponte Perro Barberia",
				"Cortes modernos",
				"https://cdn.example.com/logo.png",
				true
		);

		when(publicBarberShopRepositoryPort.findActiveCompanyBySlug("ponte-perro"))
				.thenReturn(Optional.of(profile));

		PublicBarberShopResponse response = service.getBySlug("Ponte-Perro");

		assertThat(response.slug()).isEqualTo("ponte-perro");
		assertThat(response.description()).isEqualTo("Cortes modernos");
	}

	@Test
	void inactiveCompanyIsNotReturned() {
		PublicBarberShopService service = new PublicBarberShopService(publicBarberShopRepositoryPort);

		when(publicBarberShopRepositoryPort.findActiveCompanyBySlug("inactive-shop"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getBySlug("inactive-shop"))
				.isInstanceOf(PublicResourceNotFoundException.class)
				.hasMessage("Public barber shop not found");
	}

	@Test
	void activeBranchByCompanyAndBranchSlugReturnsPublicData() {
		PublicBarberShopService service = new PublicBarberShopService(publicBarberShopRepositoryPort);
		PublicBranchResponse branch = new PublicBranchResponse("neiva-centro", "Neiva Centro", "Calle 1", "3001234567");

		when(publicBarberShopRepositoryPort.findActiveBranchBySlugs("ponte-perro", "neiva-centro"))
				.thenReturn(Optional.of(branch));

		PublicBranchResponse response = service.getBySlug("ponte-perro", "neiva-centro");

		assertThat(response.slug()).isEqualTo("neiva-centro");
		assertThat(response.phone()).isEqualTo("3001234567");
	}

	@Test
	void branchFromAnotherCompanyIsNotReturned() {
		PublicBarberShopService service = new PublicBarberShopService(publicBarberShopRepositoryPort);

		when(publicBarberShopRepositoryPort.findActiveBranchBySlugs("ponte-perro", "other-branch"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getBySlug("ponte-perro", "other-branch"))
				.isInstanceOf(PublicResourceNotFoundException.class)
				.hasMessage("Public branch not found");
	}

	@Test
	void publicServicesRequireAnActiveBranchScope() {
		PublicBarberShopService service = new PublicBarberShopService(publicBarberShopRepositoryPort);
		PublicBranchResponse branch = new PublicBranchResponse("neiva-centro", "Neiva Centro", null, null);
		PublicServiceOfferingResponse serviceOffering = new PublicServiceOfferingResponse(
				1L,
				"Corte clasico",
				"Corte tradicional",
				30,
				new BigDecimal("25000.00")
		);

		when(publicBarberShopRepositoryPort.findActiveBranchBySlugs("ponte-perro", "neiva-centro"))
				.thenReturn(Optional.of(branch));
		when(publicBarberShopRepositoryPort.findVisibleServicesByBranchSlugs("ponte-perro", "neiva-centro"))
				.thenReturn(List.of(serviceOffering));

		List<PublicServiceOfferingResponse> response = service.listServicesByBranchSlug("ponte-perro", "neiva-centro");

		assertThat(response).containsExactly(serviceOffering);
		verify(publicBarberShopRepositoryPort).findVisibleServicesByBranchSlugs("ponte-perro", "neiva-centro");
	}

	@Test
	void publicBarbersRequireAnActiveBranchScope() {
		PublicBarberShopService service = new PublicBarberShopService(publicBarberShopRepositoryPort);
		PublicBranchResponse branch = new PublicBranchResponse("neiva-centro", "Neiva Centro", null, null);
		PublicBarberResponse barber = new PublicBarberResponse(
				1L,
				"Santiago",
				"https://cdn.example.com/santiago.png",
				"Especialista en fade",
				"Fade, barba"
		);

		when(publicBarberShopRepositoryPort.findActiveBranchBySlugs("ponte-perro", "neiva-centro"))
				.thenReturn(Optional.of(branch));
		when(publicBarberShopRepositoryPort.findVisibleBarbersByBranchSlugs("ponte-perro", "neiva-centro"))
				.thenReturn(List.of(barber));

		List<PublicBarberResponse> response = service.listBarbersByBranchSlug("ponte-perro", "neiva-centro");

		assertThat(response).containsExactly(barber);
		verify(publicBarberShopRepositoryPort).findVisibleBarbersByBranchSlugs("ponte-perro", "neiva-centro");
	}
}
