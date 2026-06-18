package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.villamil.barberbooking.application.dto.response.AppointmentResponse;
import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.application.port.out.CurrentUserProvider;
import com.villamil.barberbooking.domain.exception.AppointmentInvalidStatusTransitionException;
import com.villamil.barberbooking.domain.exception.AppointmentNotFoundException;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;
import com.villamil.barberbooking.domain.model.Appointment;
import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

@ExtendWith(MockitoExtension.class)
class AppointmentLifecycleServiceTest {

	private static final Instant CREATED_AT = Instant.parse("2026-06-17T12:00:00Z");
	private static final LocalDateTime START_AT = LocalDateTime.of(2026, 6, 22, 9, 0);

	@Mock
	private AppointmentRepositoryPort appointmentRepositoryPort;

	@Mock
	private CurrentUserProvider currentUserProvider;

	@Test
	void startScheduledAppointment() {
		AppointmentLifecycleService service = service();

		when(appointmentRepositoryPort.findById(1L))
				.thenReturn(Optional.of(appointment(AppointmentStatus.SCHEDULED)));
		when(appointmentRepositoryPort.save(any(Appointment.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		AppointmentResponse response = service.start(1L);

		assertThat(response.status()).isEqualTo(AppointmentStatus.IN_PROGRESS);
		assertThat(response.updatedAt()).isNotNull();
	}

	@Test
	void failWhenStartCancelledAppointment() {
		AppointmentLifecycleService service = service();

		when(appointmentRepositoryPort.findById(1L))
				.thenReturn(Optional.of(appointment(AppointmentStatus.CANCELLED)));

		assertThatThrownBy(() -> service.start(1L))
				.isInstanceOf(AppointmentInvalidStatusTransitionException.class)
				.hasMessage("Only scheduled appointments can be started");
		verify(appointmentRepositoryPort, never()).save(any(Appointment.class));
	}

	@Test
	void completeInProgressAppointment() {
		AppointmentLifecycleService service = service();

		when(appointmentRepositoryPort.findById(1L))
				.thenReturn(Optional.of(appointment(AppointmentStatus.IN_PROGRESS)));
		when(appointmentRepositoryPort.save(any(Appointment.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		AppointmentResponse response = service.complete(1L);

		assertThat(response.status()).isEqualTo(AppointmentStatus.COMPLETED);
		assertThat(response.updatedAt()).isNotNull();
	}

	@Test
	void failWhenCompleteScheduledAppointment() {
		AppointmentLifecycleService service = service();

		when(appointmentRepositoryPort.findById(1L))
				.thenReturn(Optional.of(appointment(AppointmentStatus.SCHEDULED)));

		assertThatThrownBy(() -> service.complete(1L))
				.isInstanceOf(AppointmentInvalidStatusTransitionException.class)
				.hasMessage("Only in-progress appointments can be completed");
		verify(appointmentRepositoryPort, never()).save(any(Appointment.class));
	}

	@Test
	void markScheduledAppointmentAsNoShow() {
		AppointmentLifecycleService service = service();

		when(appointmentRepositoryPort.findById(1L))
				.thenReturn(Optional.of(appointment(AppointmentStatus.SCHEDULED)));
		when(appointmentRepositoryPort.save(any(Appointment.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		AppointmentResponse response = service.markNoShow(1L);

		assertThat(response.status()).isEqualTo(AppointmentStatus.NO_SHOW);
		assertThat(response.updatedAt()).isNotNull();
	}

	@Test
	void failWhenMarkCompletedAppointmentAsNoShow() {
		AppointmentLifecycleService service = service();

		when(appointmentRepositoryPort.findById(1L))
				.thenReturn(Optional.of(appointment(AppointmentStatus.COMPLETED)));

		assertThatThrownBy(() -> service.markNoShow(1L))
				.isInstanceOf(AppointmentInvalidStatusTransitionException.class)
				.hasMessage("Only scheduled appointments can be marked as no-show");
		verify(appointmentRepositoryPort, never()).save(any(Appointment.class));
	}

	@Test
	void failWhenAppointmentDoesNotExist() {
		AppointmentLifecycleService service = service();

		when(appointmentRepositoryPort.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.start(99L))
				.isInstanceOf(AppointmentNotFoundException.class)
				.hasMessage("Appointment not found");
	}

	@Test
	void barberCanStartOwnAppointment() {
		AppointmentLifecycleService service = securedService(barberUser(2L));
		when(appointmentRepositoryPort.findById(1L))
				.thenReturn(Optional.of(appointment(AppointmentStatus.SCHEDULED)));
		when(appointmentRepositoryPort.save(any(Appointment.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		AppointmentResponse response = service.start(1L);

		assertThat(response.status()).isEqualTo(AppointmentStatus.IN_PROGRESS);
	}

	@Test
	void barberCannotStartAnotherBarberAppointment() {
		AppointmentLifecycleService service = securedService(barberUser(9L));
		when(appointmentRepositoryPort.findById(1L))
				.thenReturn(Optional.of(appointment(AppointmentStatus.SCHEDULED)));

		assertThatThrownBy(() -> service.start(1L))
				.isInstanceOf(ForbiddenOperationException.class)
				.hasMessage("User cannot operate this appointment");
		verify(appointmentRepositoryPort, never()).save(any(Appointment.class));
	}

	private AppointmentLifecycleService service() {
		return new AppointmentLifecycleService(appointmentRepositoryPort);
	}

	private AppointmentLifecycleService securedService(AuthenticatedUserResponse actor) {
		when(currentUserProvider.currentUser()).thenReturn(Optional.of(actor));
		return new AppointmentLifecycleService(
				appointmentRepositoryPort,
				new CurrentUserResolver(currentUserProvider),
				new UserAuthorizationPolicy()
		);
	}

	private AuthenticatedUserResponse barberUser(Long barberId) {
		return new AuthenticatedUserResponse(
				10L,
				"barber@example.com",
				"Barber User",
				1L,
				1L,
				barberId,
				Set.of(Role.BARBER)
		);
	}

	private Appointment appointment(AppointmentStatus status) {
		return new Appointment(
				1L,
				1L,
				2L,
				3L,
				START_AT,
				START_AT.plusMinutes(30),
				status,
				AppointmentSource.ONLINE,
				CREATED_AT,
				null
		);
	}
}
