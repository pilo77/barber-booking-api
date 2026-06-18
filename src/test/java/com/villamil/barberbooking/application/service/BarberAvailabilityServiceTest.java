package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.villamil.barberbooking.application.dto.command.GetBarberAvailabilityCommand;
import com.villamil.barberbooking.application.dto.response.BarberAvailabilityResponse;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;
import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;
import com.villamil.barberbooking.domain.exception.ResourceInactiveException;
import com.villamil.barberbooking.domain.exception.ServiceOfferingNotFoundException;
import com.villamil.barberbooking.domain.model.Appointment;
import com.villamil.barberbooking.domain.model.Barber;
import com.villamil.barberbooking.domain.model.BarberWorkingHour;
import com.villamil.barberbooking.domain.model.ServiceOffering;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;
import com.villamil.barberbooking.domain.valueobject.AvailabilitySlotColor;
import com.villamil.barberbooking.domain.valueobject.AvailabilitySlotStatus;

@ExtendWith(MockitoExtension.class)
class BarberAvailabilityServiceTest {

	private static final Instant CREATED_AT = Instant.parse("2026-06-17T12:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-06-17T12:30:00Z");
	private static final LocalDate DATE = LocalDate.of(2026, 6, 22);

	@Mock
	private BarberRepositoryPort barberRepositoryPort;

	@Mock
	private ServiceOfferingRepositoryPort serviceOfferingRepositoryPort;

	@Mock
	private BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort;

	@Mock
	private AppointmentRepositoryPort appointmentRepositoryPort;

	@Test
	void getAvailabilityMarksAvailableAndOccupiedSlots() {
		GetBarberAvailabilityService service = service(15);

		mockActiveDependencies(30);
		when(barberWorkingHourRepositoryPort.findActiveByBarberIdAndDay(2L, DayOfWeek.MONDAY))
				.thenReturn(List.of(workingHour(LocalTime.of(8, 0), LocalTime.of(9, 0))));
		when(appointmentRepositoryPort.findByBarberIdAndDate(2L, DATE))
				.thenReturn(List.of(appointment(AppointmentStatus.SCHEDULED, LocalTime.of(8, 30), LocalTime.of(9, 0))));

		BarberAvailabilityResponse response = service.getAvailability(command());

		assertThat(response.barberId()).isEqualTo(2L);
		assertThat(response.serviceOfferingId()).isEqualTo(3L);
		assertThat(response.date()).isEqualTo(DATE);
		assertThat(response.slots()).hasSize(3);
		assertThat(response.slots().get(0).startAt()).isEqualTo(LocalDateTime.of(DATE, LocalTime.of(8, 0)));
		assertThat(response.slots().get(0).endAt()).isEqualTo(LocalDateTime.of(DATE, LocalTime.of(8, 30)));
		assertThat(response.slots().get(0).status()).isEqualTo(AvailabilitySlotStatus.AVAILABLE);
		assertThat(response.slots().get(0).color()).isEqualTo(AvailabilitySlotColor.GREEN);
		assertThat(response.slots().get(0).available()).isTrue();
		assertThat(response.slots().get(1).status()).isEqualTo(AvailabilitySlotStatus.OCCUPIED);
		assertThat(response.slots().get(1).color()).isEqualTo(AvailabilitySlotColor.RED);
		assertThat(response.slots().get(1).available()).isFalse();
		assertThat(response.slots().get(2).status()).isEqualTo(AvailabilitySlotStatus.OCCUPIED);
	}

	@Test
	void cancelledCompletedAndNoShowAppointmentsDoNotBlockAvailability() {
		GetBarberAvailabilityService service = service(15);

		mockActiveDependencies(30);
		when(barberWorkingHourRepositoryPort.findActiveByBarberIdAndDay(2L, DayOfWeek.MONDAY))
				.thenReturn(List.of(workingHour(LocalTime.of(8, 0), LocalTime.of(8, 30))));
		when(appointmentRepositoryPort.findByBarberIdAndDate(2L, DATE))
				.thenReturn(List.of(
						appointment(AppointmentStatus.CANCELLED, LocalTime.of(8, 0), LocalTime.of(8, 30)),
						appointment(AppointmentStatus.COMPLETED, LocalTime.of(8, 0), LocalTime.of(8, 30)),
						appointment(AppointmentStatus.NO_SHOW, LocalTime.of(8, 0), LocalTime.of(8, 30))
				));

		BarberAvailabilityResponse response = service.getAvailability(command());

		assertThat(response.slots()).hasSize(1);
		assertThat(response.slots().getFirst().status()).isEqualTo(AvailabilitySlotStatus.AVAILABLE);
		assertThat(response.slots().getFirst().available()).isTrue();
	}

	@Test
	void returnEmptySlotsWhenBarberHasNoActiveWorkingHours() {
		GetBarberAvailabilityService service = service(15);

		mockActiveDependencies(30);
		when(barberWorkingHourRepositoryPort.findActiveByBarberIdAndDay(2L, DayOfWeek.MONDAY))
				.thenReturn(List.of());

		BarberAvailabilityResponse response = service.getAvailability(command());

		assertThat(response.slots()).isEmpty();
		verify(appointmentRepositoryPort, never()).findByBarberIdAndDate(any(Long.class), any(LocalDate.class));
	}

	@Test
	void failWhenBarberDoesNotExist() {
		GetBarberAvailabilityService service = service(15);

		when(barberRepositoryPort.findById(2L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getAvailability(command()))
				.isInstanceOf(BarberNotFoundException.class)
				.hasMessage("Barber not found");
	}

	@Test
	void failWhenBarberIsInactive() {
		GetBarberAvailabilityService service = service(15);

		when(barberRepositoryPort.findById(2L)).thenReturn(Optional.of(barber(false)));

		assertThatThrownBy(() -> service.getAvailability(command()))
				.isInstanceOf(ResourceInactiveException.class)
				.hasMessage("Barber must be active");
	}

	@Test
	void failWhenServiceOfferingDoesNotExist() {
		GetBarberAvailabilityService service = service(15);

		when(barberRepositoryPort.findById(2L)).thenReturn(Optional.of(barber(true)));
		when(serviceOfferingRepositoryPort.findById(3L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getAvailability(command()))
				.isInstanceOf(ServiceOfferingNotFoundException.class)
				.hasMessage("Service offering not found");
	}

	@Test
	void failWhenServiceOfferingIsInactive() {
		GetBarberAvailabilityService service = service(15);

		when(barberRepositoryPort.findById(2L)).thenReturn(Optional.of(barber(true)));
		when(serviceOfferingRepositoryPort.findById(3L)).thenReturn(Optional.of(serviceOffering(30, false)));

		assertThatThrownBy(() -> service.getAvailability(command()))
				.isInstanceOf(ResourceInactiveException.class)
				.hasMessage("Service offering must be active");
	}

	@Test
	void rejectInvalidSlotStepConfiguration() {
		assertThatThrownBy(() -> service(0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Booking slot step minutes must be positive");
	}

	private GetBarberAvailabilityService service(int slotStepMinutes) {
		return new GetBarberAvailabilityService(
				barberRepositoryPort,
				serviceOfferingRepositoryPort,
				barberWorkingHourRepositoryPort,
				appointmentRepositoryPort,
				slotStepMinutes
		);
	}

	private GetBarberAvailabilityCommand command() {
		return new GetBarberAvailabilityCommand(2L, 3L, DATE);
	}

	private void mockActiveDependencies(int durationMinutes) {
		when(barberRepositoryPort.findById(2L)).thenReturn(Optional.of(barber(true)));
		when(serviceOfferingRepositoryPort.findById(3L))
				.thenReturn(Optional.of(serviceOffering(durationMinutes, true)));
	}

	private Barber barber(boolean active) {
		return new Barber(2L, "Carlos Gomez", "3101234567", "carlos@example.com", active, CREATED_AT, UPDATED_AT);
	}

	private ServiceOffering serviceOffering(int durationMinutes, boolean active) {
		return new ServiceOffering(
				3L,
				"Corte clasico",
				"Corte tradicional",
				durationMinutes,
				new BigDecimal("25000.00"),
				active,
				CREATED_AT,
				UPDATED_AT
		);
	}

	private BarberWorkingHour workingHour(LocalTime startTime, LocalTime endTime) {
		return new BarberWorkingHour(
				1L,
				2L,
				DayOfWeek.MONDAY,
				startTime,
				endTime,
				true,
				CREATED_AT,
				UPDATED_AT
		);
	}

	private Appointment appointment(AppointmentStatus status, LocalTime startTime, LocalTime endTime) {
		return new Appointment(
				1L,
				1L,
				2L,
				3L,
				LocalDateTime.of(DATE, startTime),
				LocalDateTime.of(DATE, endTime),
				status,
				AppointmentSource.ONLINE,
				CREATED_AT,
				null
		);
	}
}
