package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.villamil.barberbooking.application.dto.command.CreateBarberWorkingHourCommand;
import com.villamil.barberbooking.application.dto.command.UpdateBarberWorkingHourCommand;
import com.villamil.barberbooking.application.dto.response.BarberWorkingHourResponse;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberWorkingHourNotFoundException;
import com.villamil.barberbooking.domain.exception.BarberWorkingHourOverlapException;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.model.Barber;
import com.villamil.barberbooking.domain.model.BarberWorkingHour;

@ExtendWith(MockitoExtension.class)
class BarberWorkingHourServiceTest {

	private static final Instant CREATED_AT = Instant.parse("2026-06-17T12:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-06-17T12:30:00Z");

	@Mock
	private BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort;

	@Mock
	private BarberRepositoryPort barberRepositoryPort;

	private BarberWorkingHourBarberValidator barberValidator;
	private BarberWorkingHourOverlapValidator overlapValidator;

	@BeforeEach
	void setUp() {
		barberValidator = new BarberWorkingHourBarberValidator(barberRepositoryPort);
		overlapValidator = new BarberWorkingHourOverlapValidator(barberWorkingHourRepositoryPort);
	}

	@Test
	void createWorkingHourSuccessfully() {
		CreateBarberWorkingHourService service = new CreateBarberWorkingHourService(
				barberWorkingHourRepositoryPort,
				barberValidator,
				overlapValidator
		);
		CreateBarberWorkingHourCommand command = new CreateBarberWorkingHourCommand(
				DayOfWeek.MONDAY,
				LocalTime.of(8, 0),
				LocalTime.of(12, 0)
		);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(true)));
		when(barberWorkingHourRepositoryPort.findActiveByBarberIdAndDay(1L, DayOfWeek.MONDAY)).thenReturn(List.of());
		when(barberWorkingHourRepositoryPort.save(any(BarberWorkingHour.class))).thenReturn(workingHour(1L, true));

		BarberWorkingHourResponse response = service.create(1L, command);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.barberId()).isEqualTo(1L);
		assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
		assertThat(response.active()).isTrue();
	}

	@Test
	void rejectCreateWhenBarberIsInactive() {
		CreateBarberWorkingHourService service = new CreateBarberWorkingHourService(
				barberWorkingHourRepositoryPort,
				barberValidator,
				overlapValidator
		);
		CreateBarberWorkingHourCommand command = new CreateBarberWorkingHourCommand(
				DayOfWeek.MONDAY,
				LocalTime.of(8, 0),
				LocalTime.of(12, 0)
		);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(false)));

		assertThatThrownBy(() -> service.create(1L, command))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Barber must be active");
		verify(barberWorkingHourRepositoryPort, never()).save(any(BarberWorkingHour.class));
	}

	@Test
	void rejectCreateWhenWorkingHourOverlapsActiveWorkingHour() {
		CreateBarberWorkingHourService service = new CreateBarberWorkingHourService(
				barberWorkingHourRepositoryPort,
				barberValidator,
				overlapValidator
		);
		CreateBarberWorkingHourCommand command = new CreateBarberWorkingHourCommand(
				DayOfWeek.MONDAY,
				LocalTime.of(11, 0),
				LocalTime.of(15, 0)
		);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(true)));
		when(barberWorkingHourRepositoryPort.findActiveByBarberIdAndDay(1L, DayOfWeek.MONDAY))
				.thenReturn(List.of(workingHour(2L, true)));

		assertThatThrownBy(() -> service.create(1L, command))
				.isInstanceOf(BarberWorkingHourOverlapException.class)
				.hasMessage("Working hour overlaps with an active working hour");
		verify(barberWorkingHourRepositoryPort, never()).save(any(BarberWorkingHour.class));
	}

	@Test
	void rejectInvalidTimeRange() {
		assertThatThrownBy(() -> BarberWorkingHour.create(
				1L,
				DayOfWeek.MONDAY,
				LocalTime.of(12, 0),
				LocalTime.of(8, 0)
		))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Working hour start time must be before end time");
	}

	@Test
	void getExistingWorkingHour() {
		GetBarberWorkingHourService service = new GetBarberWorkingHourService(
				barberWorkingHourRepositoryPort,
				barberValidator
		);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(true)));
		when(barberWorkingHourRepositoryPort.findByIdAndBarberId(1L, 1L)).thenReturn(Optional.of(workingHour(1L, true)));

		BarberWorkingHourResponse response = service.getById(1L, 1L);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.startTime()).isEqualTo(LocalTime.of(8, 0));
	}

	@Test
	void failWhenWorkingHourDoesNotExist() {
		GetBarberWorkingHourService service = new GetBarberWorkingHourService(
				barberWorkingHourRepositoryPort,
				barberValidator
		);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(true)));
		when(barberWorkingHourRepositoryPort.findByIdAndBarberId(99L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(1L, 99L))
				.isInstanceOf(BarberWorkingHourNotFoundException.class)
				.hasMessage("Working hour not found");
	}

	@Test
	void listWorkingHours() {
		ListBarberWorkingHoursService service = new ListBarberWorkingHoursService(
				barberWorkingHourRepositoryPort,
				barberValidator
		);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(true)));
		when(barberWorkingHourRepositoryPort.findAllByBarberId(1L)).thenReturn(List.of(workingHour(1L, true)));

		List<BarberWorkingHourResponse> response = service.list(1L);

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().barberId()).isEqualTo(1L);
	}

	@Test
	void updateWorkingHourSuccessfully() {
		UpdateBarberWorkingHourService service = new UpdateBarberWorkingHourService(
				barberWorkingHourRepositoryPort,
				barberValidator,
				overlapValidator
		);
		UpdateBarberWorkingHourCommand command = new UpdateBarberWorkingHourCommand(
				DayOfWeek.TUESDAY,
				LocalTime.of(14, 0),
				LocalTime.of(18, 0)
		);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(true)));
		when(barberWorkingHourRepositoryPort.findByIdAndBarberId(1L, 1L)).thenReturn(Optional.of(workingHour(1L, true)));
		when(barberWorkingHourRepositoryPort.findActiveByBarberIdAndDay(1L, DayOfWeek.TUESDAY)).thenReturn(List.of());
		when(barberWorkingHourRepositoryPort.save(any(BarberWorkingHour.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		BarberWorkingHourResponse response = service.update(1L, 1L, command);

		assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
		assertThat(response.startTime()).isEqualTo(LocalTime.of(14, 0));
		assertThat(response.endTime()).isEqualTo(LocalTime.of(18, 0));
	}

	@Test
	void rejectUpdateWhenWorkingHourOverlapsAnotherActiveWorkingHour() {
		UpdateBarberWorkingHourService service = new UpdateBarberWorkingHourService(
				barberWorkingHourRepositoryPort,
				barberValidator,
				overlapValidator
		);
		UpdateBarberWorkingHourCommand command = new UpdateBarberWorkingHourCommand(
				DayOfWeek.MONDAY,
				LocalTime.of(11, 0),
				LocalTime.of(15, 0)
		);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(true)));
		when(barberWorkingHourRepositoryPort.findByIdAndBarberId(1L, 1L)).thenReturn(Optional.of(workingHour(1L, true)));
		when(barberWorkingHourRepositoryPort.findActiveByBarberIdAndDay(1L, DayOfWeek.MONDAY))
				.thenReturn(List.of(workingHour(1L, true), workingHour(2L, true)));

		assertThatThrownBy(() -> service.update(1L, 1L, command))
				.isInstanceOf(BarberWorkingHourOverlapException.class)
				.hasMessage("Working hour overlaps with an active working hour");
	}

	@Test
	void activateWorkingHourSuccessfully() {
		ActivateBarberWorkingHourService service = new ActivateBarberWorkingHourService(
				barberWorkingHourRepositoryPort,
				barberValidator,
				overlapValidator
		);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(true)));
		when(barberWorkingHourRepositoryPort.findByIdAndBarberId(1L, 1L)).thenReturn(Optional.of(workingHour(1L, false)));
		when(barberWorkingHourRepositoryPort.findActiveByBarberIdAndDay(1L, DayOfWeek.MONDAY)).thenReturn(List.of());
		when(barberWorkingHourRepositoryPort.save(any(BarberWorkingHour.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		BarberWorkingHourResponse response = service.activate(1L, 1L);

		assertThat(response.active()).isTrue();
	}

	@Test
	void rejectActivateWhenWorkingHourOverlapsActiveWorkingHour() {
		ActivateBarberWorkingHourService service = new ActivateBarberWorkingHourService(
				barberWorkingHourRepositoryPort,
				barberValidator,
				overlapValidator
		);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(true)));
		when(barberWorkingHourRepositoryPort.findByIdAndBarberId(1L, 1L)).thenReturn(Optional.of(workingHour(1L, false)));
		when(barberWorkingHourRepositoryPort.findActiveByBarberIdAndDay(1L, DayOfWeek.MONDAY))
				.thenReturn(List.of(workingHour(2L, true)));

		assertThatThrownBy(() -> service.activate(1L, 1L))
				.isInstanceOf(BarberWorkingHourOverlapException.class);
	}

	@Test
	void deactivateWorkingHour() {
		DeactivateBarberWorkingHourService service = new DeactivateBarberWorkingHourService(
				barberWorkingHourRepositoryPort,
				barberValidator
		);

		when(barberRepositoryPort.findById(1L)).thenReturn(Optional.of(barber(true)));
		when(barberWorkingHourRepositoryPort.findByIdAndBarberId(1L, 1L)).thenReturn(Optional.of(workingHour(1L, true)));
		when(barberWorkingHourRepositoryPort.save(any(BarberWorkingHour.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		BarberWorkingHourResponse response = service.deactivate(1L, 1L);

		assertThat(response.active()).isFalse();
	}

	private Barber barber(boolean active) {
		return new Barber(1L, "Carlos Gomez", "3101234567", "carlos@example.com", active, CREATED_AT, UPDATED_AT);
	}

	private BarberWorkingHour workingHour(Long id, boolean active) {
		return new BarberWorkingHour(
				id,
				1L,
				DayOfWeek.MONDAY,
				LocalTime.of(8, 0),
				LocalTime.of(12, 0),
				active,
				CREATED_AT,
				UPDATED_AT
		);
	}
}
