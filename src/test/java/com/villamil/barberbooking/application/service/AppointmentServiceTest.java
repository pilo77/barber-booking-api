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

import com.villamil.barberbooking.application.dto.command.BookAppointmentCommand;
import com.villamil.barberbooking.application.dto.command.CreateWalkInAppointmentCommand;
import com.villamil.barberbooking.application.dto.response.AppointmentResponse;
import com.villamil.barberbooking.application.dto.response.BarberDailyScheduleResponse;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;
import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;
import com.villamil.barberbooking.domain.exception.AppointmentNotAvailableException;
import com.villamil.barberbooking.domain.exception.AppointmentInvalidStatusTransitionException;
import com.villamil.barberbooking.domain.exception.AppointmentNotFoundException;
import com.villamil.barberbooking.domain.exception.AppointmentOutsideWorkingHoursException;
import com.villamil.barberbooking.domain.exception.CustomerNotFoundException;
import com.villamil.barberbooking.domain.exception.ResourceInactiveException;
import com.villamil.barberbooking.domain.model.Appointment;
import com.villamil.barberbooking.domain.model.Barber;
import com.villamil.barberbooking.domain.model.BarberWorkingHour;
import com.villamil.barberbooking.domain.model.Customer;
import com.villamil.barberbooking.domain.model.ServiceOffering;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

	private static final Instant CREATED_AT = Instant.parse("2026-06-17T12:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-06-17T12:30:00Z");
	private static final LocalDateTime START_AT = LocalDateTime.of(2026, 6, 22, 9, 0);

	@Mock
	private AppointmentRepositoryPort appointmentRepositoryPort;

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@Mock
	private BarberRepositoryPort barberRepositoryPort;

	@Mock
	private ServiceOfferingRepositoryPort serviceOfferingRepositoryPort;

	@Mock
	private BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort;

	@Test
	void bookAppointmentSuccessfully() {
		BookAppointmentService service = service();
		BookAppointmentCommand command = command();

		mockActiveDependencies();
		when(barberWorkingHourRepositoryPort.findActiveByBarberIdAndDay(2L, DayOfWeek.MONDAY))
				.thenReturn(List.of(workingHour()));
		when(appointmentRepositoryPort.existsBlockingOverlap(2L, START_AT, START_AT.plusMinutes(30))).thenReturn(false);
		when(appointmentRepositoryPort.save(any(Appointment.class)))
				.thenAnswer(invocation -> withId(invocation.getArgument(0)));

		AppointmentResponse response = service.book(command);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.serviceOfferingId()).isEqualTo(3L);
		assertThat(response.endAt()).isEqualTo(START_AT.plusMinutes(30));
		assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
		assertThat(response.source()).isEqualTo(AppointmentSource.ONLINE);
	}

	@Test
	void failWhenCustomerDoesNotExist() {
		BookAppointmentService service = service();

		when(customerRepositoryPort.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.book(command()))
				.isInstanceOf(CustomerNotFoundException.class)
				.hasMessage("Customer not found");
		verify(appointmentRepositoryPort, never()).save(any(Appointment.class));
	}

	@Test
	void failWhenCustomerIsInactive() {
		BookAppointmentService service = service();

		when(customerRepositoryPort.findById(1L)).thenReturn(Optional.of(customer(false)));

		assertThatThrownBy(() -> service.book(command()))
				.isInstanceOf(ResourceInactiveException.class)
				.hasMessage("Customer must be active");
		verify(appointmentRepositoryPort, never()).save(any(Appointment.class));
	}

	@Test
	void failWhenBarberIsInactive() {
		BookAppointmentService service = service();

		when(customerRepositoryPort.findById(1L)).thenReturn(Optional.of(customer(true)));
		when(barberRepositoryPort.findById(2L)).thenReturn(Optional.of(barber(false)));

		assertThatThrownBy(() -> service.book(command()))
				.isInstanceOf(ResourceInactiveException.class)
				.hasMessage("Barber must be active");
		verify(appointmentRepositoryPort, never()).save(any(Appointment.class));
	}

	@Test
	void failWhenServiceOfferingIsInactive() {
		BookAppointmentService service = service();

		when(customerRepositoryPort.findById(1L)).thenReturn(Optional.of(customer(true)));
		when(barberRepositoryPort.findById(2L)).thenReturn(Optional.of(barber(true)));
		when(serviceOfferingRepositoryPort.findById(3L)).thenReturn(Optional.of(serviceOffering(false)));

		assertThatThrownBy(() -> service.book(command()))
				.isInstanceOf(ResourceInactiveException.class)
				.hasMessage("Service offering must be active");
		verify(appointmentRepositoryPort, never()).save(any(Appointment.class));
	}

	@Test
	void failWhenAppointmentIsOutsideWorkingHours() {
		BookAppointmentService service = service();

		mockActiveDependencies();
		when(barberWorkingHourRepositoryPort.findActiveByBarberIdAndDay(2L, DayOfWeek.MONDAY))
				.thenReturn(List.of());

		assertThatThrownBy(() -> service.book(command()))
				.isInstanceOf(AppointmentOutsideWorkingHoursException.class)
				.hasMessage("Appointment is outside barber working hours");
		verify(appointmentRepositoryPort, never()).save(any(Appointment.class));
	}

	@Test
	void failWhenAppointmentOverlapsBlockingAppointment() {
		BookAppointmentService service = service();

		mockActiveDependencies();
		when(barberWorkingHourRepositoryPort.findActiveByBarberIdAndDay(2L, DayOfWeek.MONDAY))
				.thenReturn(List.of(workingHour()));
		when(appointmentRepositoryPort.existsBlockingOverlap(2L, START_AT, START_AT.plusMinutes(30))).thenReturn(true);

		assertThatThrownBy(() -> service.book(command()))
				.isInstanceOf(AppointmentNotAvailableException.class)
				.hasMessage("Appointment overlaps with an active appointment");
		verify(appointmentRepositoryPort, never()).save(any(Appointment.class));
	}

	@Test
	void createWalkInAppointmentImmediately() {
		CreateWalkInAppointmentService service = walkInService();

		mockActiveDependencies();
		when(barberWorkingHourRepositoryPort.findActiveByBarberIdAndDay(2L, DayOfWeek.MONDAY))
				.thenReturn(List.of(workingHour()));
		when(appointmentRepositoryPort.existsBlockingOverlap(2L, START_AT, START_AT.plusMinutes(30))).thenReturn(false);
		when(appointmentRepositoryPort.save(any(Appointment.class)))
				.thenAnswer(invocation -> withId(invocation.getArgument(0)));

		AppointmentResponse response = service.create(walkInCommand(true));

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.status()).isEqualTo(AppointmentStatus.IN_PROGRESS);
		assertThat(response.source()).isEqualTo(AppointmentSource.WALK_IN);
		assertThat(response.endAt()).isEqualTo(START_AT.plusMinutes(30));
	}

	@Test
	void createWalkInAppointmentScheduled() {
		CreateWalkInAppointmentService service = walkInService();

		mockActiveDependencies();
		when(barberWorkingHourRepositoryPort.findActiveByBarberIdAndDay(2L, DayOfWeek.MONDAY))
				.thenReturn(List.of(workingHour()));
		when(appointmentRepositoryPort.existsBlockingOverlap(2L, START_AT, START_AT.plusMinutes(30))).thenReturn(false);
		when(appointmentRepositoryPort.save(any(Appointment.class)))
				.thenAnswer(invocation -> withId(invocation.getArgument(0)));

		AppointmentResponse response = service.create(walkInCommand(false));

		assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
		assertThat(response.source()).isEqualTo(AppointmentSource.WALK_IN);
	}

	@Test
	void failWhenWalkInAppointmentOverlapsBlockingAppointment() {
		CreateWalkInAppointmentService service = walkInService();

		mockActiveDependencies();
		when(barberWorkingHourRepositoryPort.findActiveByBarberIdAndDay(2L, DayOfWeek.MONDAY))
				.thenReturn(List.of(workingHour()));
		when(appointmentRepositoryPort.existsBlockingOverlap(2L, START_AT, START_AT.plusMinutes(30))).thenReturn(true);

		assertThatThrownBy(() -> service.create(walkInCommand(true)))
				.isInstanceOf(AppointmentNotAvailableException.class)
				.hasMessage("Appointment overlaps with an active appointment");
		verify(appointmentRepositoryPort, never()).save(any(Appointment.class));
	}

	@Test
	void cancelAppointmentSuccessfully() {
		CancelAppointmentService service = new CancelAppointmentService(appointmentRepositoryPort);
		Appointment scheduled = appointment(AppointmentStatus.SCHEDULED);

		when(appointmentRepositoryPort.findById(1L)).thenReturn(Optional.of(scheduled));
		when(appointmentRepositoryPort.save(any(Appointment.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		AppointmentResponse response = service.cancel(1L);

		assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
		assertThat(response.updatedAt()).isNotNull();
	}

	@Test
	void failWhenCancelAppointmentDoesNotExist() {
		CancelAppointmentService service = new CancelAppointmentService(appointmentRepositoryPort);

		when(appointmentRepositoryPort.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.cancel(99L))
				.isInstanceOf(AppointmentNotFoundException.class)
				.hasMessage("Appointment not found");
	}

	@Test
	void failWhenCancelCompletedAppointment() {
		CancelAppointmentService service = new CancelAppointmentService(appointmentRepositoryPort);

		when(appointmentRepositoryPort.findById(1L))
				.thenReturn(Optional.of(appointment(AppointmentStatus.COMPLETED)));

		assertThatThrownBy(() -> service.cancel(1L))
				.isInstanceOf(AppointmentInvalidStatusTransitionException.class)
				.hasMessage("Only scheduled appointments can be cancelled");
		verify(appointmentRepositoryPort, never()).save(any(Appointment.class));
	}

	@Test
	void getBarberDailyAppointments() {
		GetBarberDailyAppointmentsService service = new GetBarberDailyAppointmentsService(
				appointmentRepositoryPort,
				barberRepositoryPort
		);
		LocalDate date = LocalDate.of(2026, 6, 22);

		when(barberRepositoryPort.findById(2L)).thenReturn(Optional.of(barber(true)));
		when(appointmentRepositoryPort.findByBarberIdAndDate(2L, date))
				.thenReturn(List.of(appointment(AppointmentStatus.SCHEDULED)));

		BarberDailyScheduleResponse response = service.getDailyAppointments(2L, date);

		assertThat(response.barberId()).isEqualTo(2L);
		assertThat(response.date()).isEqualTo(date);
		assertThat(response.appointments()).hasSize(1);
	}

	private BookAppointmentService service() {
		return new BookAppointmentService(
				appointmentRepositoryPort,
				appointmentBookingPolicy()
		);
	}

	private CreateWalkInAppointmentService walkInService() {
		return new CreateWalkInAppointmentService(
				appointmentRepositoryPort,
				appointmentBookingPolicy()
		);
	}

	private AppointmentBookingPolicy appointmentBookingPolicy() {
		return new AppointmentBookingPolicy(
				appointmentRepositoryPort,
				customerRepositoryPort,
				barberRepositoryPort,
				serviceOfferingRepositoryPort,
				barberWorkingHourRepositoryPort
		);
	}

	private BookAppointmentCommand command() {
		return new BookAppointmentCommand(1L, 2L, 3L, START_AT);
	}

	private CreateWalkInAppointmentCommand walkInCommand(boolean startImmediately) {
		return new CreateWalkInAppointmentCommand(1L, 2L, 3L, START_AT, startImmediately);
	}

	private void mockActiveDependencies() {
		when(customerRepositoryPort.findById(1L)).thenReturn(Optional.of(customer(true)));
		when(barberRepositoryPort.findById(2L)).thenReturn(Optional.of(barber(true)));
		when(serviceOfferingRepositoryPort.findById(3L)).thenReturn(Optional.of(serviceOffering(true)));
	}

	private Customer customer(boolean active) {
		return new Customer(1L, "Ana Perez", "3001234567", "ana@example.com", active, CREATED_AT, UPDATED_AT);
	}

	private Barber barber(boolean active) {
		return new Barber(2L, "Carlos Gomez", "3101234567", "carlos@example.com", active, CREATED_AT, UPDATED_AT);
	}

	private ServiceOffering serviceOffering(boolean active) {
		return new ServiceOffering(
				3L,
				"Corte clasico",
				"Corte tradicional",
				30,
				new BigDecimal("25000.00"),
				active,
				CREATED_AT,
				UPDATED_AT
		);
	}

	private BarberWorkingHour workingHour() {
		return new BarberWorkingHour(
				1L,
				2L,
				DayOfWeek.MONDAY,
				LocalTime.of(8, 0),
				LocalTime.of(12, 0),
				true,
				CREATED_AT,
				UPDATED_AT
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

	private Appointment withId(Appointment appointment) {
		return new Appointment(
				1L,
				appointment.customerId(),
				appointment.barberId(),
				appointment.serviceOfferingId(),
				appointment.startAt(),
				appointment.endAt(),
				appointment.status(),
				appointment.source(),
				appointment.createdAt(),
				appointment.updatedAt()
		);
	}
}
