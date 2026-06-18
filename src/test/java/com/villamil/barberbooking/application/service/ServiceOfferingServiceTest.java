package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.villamil.barberbooking.application.dto.command.CreateServiceOfferingCommand;
import com.villamil.barberbooking.application.dto.command.UpdateServiceOfferingCommand;
import com.villamil.barberbooking.application.dto.response.ServiceOfferingResponse;
import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.exception.ServiceOfferingAlreadyExistsException;
import com.villamil.barberbooking.domain.exception.ServiceOfferingNotFoundException;
import com.villamil.barberbooking.domain.model.ServiceOffering;

@ExtendWith(MockitoExtension.class)
class ServiceOfferingServiceTest {

	private static final Instant CREATED_AT = Instant.parse("2026-06-17T12:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-06-17T12:30:00Z");

	@Mock
	private ServiceOfferingRepositoryPort serviceOfferingRepositoryPort;

	private ServiceOfferingUniquenessValidator serviceOfferingUniquenessValidator;

	@BeforeEach
	void setUp() {
		serviceOfferingUniquenessValidator = new ServiceOfferingUniquenessValidator(serviceOfferingRepositoryPort);
	}

	@Test
	void createServiceOfferingSuccessfully() {
		CreateServiceOfferingService service = new CreateServiceOfferingService(
				serviceOfferingRepositoryPort,
				serviceOfferingUniquenessValidator
		);
		CreateServiceOfferingCommand command = new CreateServiceOfferingCommand(
				"Corte clasico",
				"Corte tradicional",
				30,
				new BigDecimal("25000.00")
		);

		when(serviceOfferingRepositoryPort.existsByName("Corte clasico")).thenReturn(false);
		when(serviceOfferingRepositoryPort.save(any(ServiceOffering.class))).thenReturn(serviceOffering(true));

		ServiceOfferingResponse response = service.create(command);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.name()).isEqualTo("Corte clasico");
		assertThat(response.active()).isTrue();
	}

	@Test
	void rejectDuplicatedServiceOfferingName() {
		CreateServiceOfferingService service = new CreateServiceOfferingService(
				serviceOfferingRepositoryPort,
				serviceOfferingUniquenessValidator
		);
		CreateServiceOfferingCommand command = new CreateServiceOfferingCommand(
				"Corte clasico",
				null,
				30,
				new BigDecimal("25000.00")
		);

		when(serviceOfferingRepositoryPort.existsByName("Corte clasico")).thenReturn(true);

		assertThatThrownBy(() -> service.create(command))
				.isInstanceOf(ServiceOfferingAlreadyExistsException.class)
				.hasMessage("Service offering name already exists");
		verify(serviceOfferingRepositoryPort, never()).save(any(ServiceOffering.class));
	}

	@Test
	void rejectBlankName() {
		CreateServiceOfferingService service = new CreateServiceOfferingService(
				serviceOfferingRepositoryPort,
				serviceOfferingUniquenessValidator
		);
		CreateServiceOfferingCommand command = new CreateServiceOfferingCommand(
				" ",
				null,
				30,
				new BigDecimal("25000.00")
		);

		assertThatThrownBy(() -> service.create(command))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Service offering name is required");
	}

	@Test
	void rejectInvalidDuration() {
		assertThatThrownBy(() -> ServiceOffering.create("Corte", null, 0, new BigDecimal("25000.00")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Service offering duration must be positive");
	}

	@Test
	void rejectNegativePrice() {
		assertThatThrownBy(() -> ServiceOffering.create("Corte", null, 30, new BigDecimal("-1.00")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Service offering price must be zero or positive");
	}

	@Test
	void rejectPriceWithMoreThanTwoDecimals() {
		assertThatThrownBy(() -> ServiceOffering.create("Corte", null, 30, new BigDecimal("25000.123")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Service offering price must have at most 2 decimal places");
	}

	@Test
	void getExistingServiceOffering() {
		GetServiceOfferingService service = new GetServiceOfferingService(serviceOfferingRepositoryPort);

		when(serviceOfferingRepositoryPort.findById(1L)).thenReturn(Optional.of(serviceOffering(true)));

		ServiceOfferingResponse response = service.getById(1L);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.durationMinutes()).isEqualTo(30);
	}

	@Test
	void failWhenServiceOfferingDoesNotExist() {
		GetServiceOfferingService service = new GetServiceOfferingService(serviceOfferingRepositoryPort);

		when(serviceOfferingRepositoryPort.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(99L))
				.isInstanceOf(ServiceOfferingNotFoundException.class)
				.hasMessage("Service offering not found");
	}

	@Test
	void listServiceOfferings() {
		ListServiceOfferingsService service = new ListServiceOfferingsService(serviceOfferingRepositoryPort);

		when(serviceOfferingRepositoryPort.findAll()).thenReturn(List.of(serviceOffering(true)));

		List<ServiceOfferingResponse> serviceOfferings = service.list();

		assertThat(serviceOfferings).hasSize(1);
		assertThat(serviceOfferings.getFirst().id()).isEqualTo(1L);
	}

	@Test
	void updateServiceOfferingSuccessfully() {
		UpdateServiceOfferingService service = new UpdateServiceOfferingService(
				serviceOfferingRepositoryPort,
				serviceOfferingUniquenessValidator
		);
		UpdateServiceOfferingCommand command = new UpdateServiceOfferingCommand(
				"Corte premium",
				"Corte con barba",
				45,
				new BigDecimal("40000.00")
		);

		when(serviceOfferingRepositoryPort.findById(1L)).thenReturn(Optional.of(serviceOffering(true)));
		when(serviceOfferingRepositoryPort.existsByNameAndIdNot("Corte premium", 1L)).thenReturn(false);
		when(serviceOfferingRepositoryPort.save(any(ServiceOffering.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		ServiceOfferingResponse response = service.update(1L, command);

		assertThat(response.name()).isEqualTo("Corte premium");
		assertThat(response.durationMinutes()).isEqualTo(45);
		assertThat(response.price()).isEqualByComparingTo("40000.00");
	}

	@Test
	void activateServiceOffering() {
		ActivateServiceOfferingService service = new ActivateServiceOfferingService(serviceOfferingRepositoryPort);

		when(serviceOfferingRepositoryPort.findById(1L)).thenReturn(Optional.of(serviceOffering(false)));
		when(serviceOfferingRepositoryPort.save(any(ServiceOffering.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		ServiceOfferingResponse response = service.activate(1L);

		assertThat(response.active()).isTrue();
	}

	@Test
	void deactivateServiceOffering() {
		DeactivateServiceOfferingService service = new DeactivateServiceOfferingService(serviceOfferingRepositoryPort);

		when(serviceOfferingRepositoryPort.findById(1L)).thenReturn(Optional.of(serviceOffering(true)));
		when(serviceOfferingRepositoryPort.save(any(ServiceOffering.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		ServiceOfferingResponse response = service.deactivate(1L);

		assertThat(response.active()).isFalse();
	}

	private ServiceOffering serviceOffering(boolean active) {
		return new ServiceOffering(
				1L,
				"Corte clasico",
				"Corte tradicional",
				30,
				new BigDecimal("25000.00"),
				active,
				CREATED_AT,
				UPDATED_AT
		);
	}
}
