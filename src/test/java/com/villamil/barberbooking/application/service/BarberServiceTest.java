package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.villamil.barberbooking.application.dto.command.CreateBarberCommand;
import com.villamil.barberbooking.application.dto.command.UpdateBarberCommand;
import com.villamil.barberbooking.application.dto.response.BarberResponse;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberAlreadyExistsException;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.model.Barber;

@ExtendWith(MockitoExtension.class)
class BarberServiceTest {

	private static final Instant CREATED_AT = Instant.parse("2026-06-17T12:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-06-17T12:30:00Z");

	@Mock
	private BarberRepositoryPort barberRepositoryPort;

	private BarberUniquenessValidator barberUniquenessValidator;

	@BeforeEach
	void setUp() {
		barberUniquenessValidator = new BarberUniquenessValidator(barberRepositoryPort);
	}

	@Test
	void createBarberSuccessfully() {
		CreateBarberService service = new CreateBarberService(barberRepositoryPort, barberUniquenessValidator);
		CreateBarberCommand command = new CreateBarberCommand("Carlos Gomez", "3101234567", "carlos@example.com");

		when(barberRepositoryPort.existsByPhone("3101234567")).thenReturn(false);
		when(barberRepositoryPort.existsByEmail("carlos@example.com")).thenReturn(false);
		when(barberRepositoryPort.save(any(Barber.class))).thenReturn(barber(true));

		BarberResponse response = service.create(command);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.fullName()).isEqualTo("Carlos Gomez");
		assertThat(response.active()).isTrue();
	}

	@Test
	void rejectBarberWithDuplicatedPhone() {
		CreateBarberService service = new CreateBarberService(barberRepositoryPort, barberUniquenessValidator);
		CreateBarberCommand command = new CreateBarberCommand("Carlos Gomez", "3101234567", null);

		when(barberRepositoryPort.existsByPhone("3101234567")).thenReturn(true);

		assertThatThrownBy(() -> service.create(command))
				.isInstanceOf(BarberAlreadyExistsException.class)
				.hasMessage("Barber phone already exists");
		verify(barberRepositoryPort, never()).save(any(Barber.class));
	}

	@Test
	void rejectBarberWithBlankName() {
		CreateBarberService service = new CreateBarberService(barberRepositoryPort, barberUniquenessValidator);
		CreateBarberCommand command = new CreateBarberCommand(" ", "3101234567", null);

		assertThatThrownBy(() -> service.create(command))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Barber full name is required");
	}

	@Test
	void getExistingBarber() {
		GetBarberService service = new GetBarberService(barberRepositoryPort);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(true)));

		BarberResponse response = service.getById(1L);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.phone()).isEqualTo("3101234567");
	}

	@Test
	void failWhenBarberDoesNotExist() {
		GetBarberService service = new GetBarberService(barberRepositoryPort);

		when(barberRepositoryPort.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(99L))
				.isInstanceOf(BarberNotFoundException.class)
				.hasMessage("Barber not found");
	}

	@Test
	void listBarbers() {
		ListBarbersService service = new ListBarbersService(barberRepositoryPort);

		when(barberRepositoryPort.findAll()).thenReturn(List.of(barber(true)));

		List<BarberResponse> barbers = service.list();

		assertThat(barbers).hasSize(1);
		assertThat(barbers.getFirst().id()).isEqualTo(1L);
	}

	@Test
	void updateBarberSuccessfully() {
		UpdateBarberService service = new UpdateBarberService(barberRepositoryPort, barberUniquenessValidator);
		UpdateBarberCommand command = new UpdateBarberCommand("Carlos Andres Gomez", "3109876543", null);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(true)));
		when(barberRepositoryPort.existsByPhoneAndIdNot("3109876543", 1L)).thenReturn(false);
		when(barberRepositoryPort.save(any(Barber.class))).thenAnswer(invocation -> invocation.getArgument(0));

		BarberResponse response = service.update(1L, command);

		assertThat(response.fullName()).isEqualTo("Carlos Andres Gomez");
		assertThat(response.phone()).isEqualTo("3109876543");
		assertThat(response.active()).isTrue();
	}

	@Test
	void rejectUpdateWithDuplicatedPhone() {
		UpdateBarberService service = new UpdateBarberService(barberRepositoryPort, barberUniquenessValidator);
		UpdateBarberCommand command = new UpdateBarberCommand("Carlos Gomez", "3109876543", null);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(true)));
		when(barberRepositoryPort.existsByPhoneAndIdNot("3109876543", 1L)).thenReturn(true);

		assertThatThrownBy(() -> service.update(1L, command))
				.isInstanceOf(BarberAlreadyExistsException.class)
				.hasMessage("Barber phone already exists");
		verify(barberRepositoryPort, never()).save(any(Barber.class));
	}

	@Test
	void activateBarber() {
		ActivateBarberService service = new ActivateBarberService(barberRepositoryPort);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(false)));
		when(barberRepositoryPort.save(any(Barber.class))).thenAnswer(invocation -> invocation.getArgument(0));

		BarberResponse response = service.activate(1L);

		assertThat(response.active()).isTrue();
	}

	@Test
	void deactivateBarber() {
		DeactivateBarberService service = new DeactivateBarberService(barberRepositoryPort);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(true)));
		when(barberRepositoryPort.save(any(Barber.class))).thenAnswer(invocation -> invocation.getArgument(0));

		BarberResponse response = service.deactivate(1L);

		assertThat(response.active()).isFalse();
	}

	private Barber barber(boolean active) {
		return new Barber(
				1L,
				"Carlos Gomez",
				"3101234567",
				"carlos@example.com",
				active,
				CREATED_AT,
				UPDATED_AT
		);
	}
}
