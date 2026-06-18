package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.villamil.barberbooking.application.dto.command.GetBarberDailyDashboardCommand;
import com.villamil.barberbooking.application.dto.response.BarberDailyDashboardResponse;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;
import com.villamil.barberbooking.domain.exception.CustomerNotFoundException;
import com.villamil.barberbooking.domain.model.Appointment;
import com.villamil.barberbooking.domain.model.Barber;
import com.villamil.barberbooking.domain.model.Customer;
import com.villamil.barberbooking.domain.model.ServiceOffering;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

@ExtendWith(MockitoExtension.class)
class BarberDailyDashboardServiceTest {

	private static final Instant CREATED_AT = Instant.parse("2026-06-17T12:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-06-17T12:30:00Z");
	private static final LocalDate DATE = LocalDate.of(2026, 6, 22);
	private static final Clock CLOCK = Clock.fixed(
			Instant.parse("2026-06-22T09:15:00Z"),
			ZoneId.of("UTC")
	);

	@Mock
	private BarberRepositoryPort barberRepositoryPort;

	@Mock
	private AppointmentRepositoryPort appointmentRepositoryPort;

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@Mock
	private ServiceOfferingRepositoryPort serviceOfferingRepositoryPort;

	@Test
	void getDailyDashboardWithSummaryAndNextAppointment() {
		GetBarberDailyDashboardService service = service();
		List<Appointment> appointments = List.of(
				appointment(5L, 15, AppointmentStatus.NO_SHOW),
				appointment(2L, 9, AppointmentStatus.COMPLETED),
				appointment(1L, 8, AppointmentStatus.SCHEDULED),
				appointment(3L, 10, AppointmentStatus.IN_PROGRESS),
				appointment(4L, 11, AppointmentStatus.CANCELLED)
		);

		when(barberRepositoryPort.findById(2L)).thenReturn(Optional.of(barber()));
		when(appointmentRepositoryPort.findByBarberIdAndDate(2L, DATE)).thenReturn(appointments);
		when(customerRepositoryPort.findById(1L)).thenReturn(Optional.of(customer(1L, "Ana Perez")));
		when(customerRepositoryPort.findById(2L)).thenReturn(Optional.of(customer(2L, "Luis Mora")));
		when(customerRepositoryPort.findById(3L)).thenReturn(Optional.of(customer(3L, "Sara Diaz")));
		when(customerRepositoryPort.findById(4L)).thenReturn(Optional.of(customer(4L, "Jose Ruiz")));
		when(customerRepositoryPort.findById(5L)).thenReturn(Optional.of(customer(5L, "Nora Cano")));
		when(serviceOfferingRepositoryPort.findById(3L)).thenReturn(Optional.of(serviceOffering()));

		BarberDailyDashboardResponse response = service.getDailyDashboard(command());

		assertThat(response.barberId()).isEqualTo(2L);
		assertThat(response.barberName()).isEqualTo("Carlos Gomez");
		assertThat(response.date()).isEqualTo(DATE);
		assertThat(response.appointments()).extracting("appointmentId")
				.containsExactly(1L, 2L, 3L, 4L, 5L);
		assertThat(response.appointments().get(0).customerName()).isEqualTo("Ana Perez");
		assertThat(response.appointments().get(0).serviceName()).isEqualTo("Corte clasico");
		assertThat(response.summary().totalAppointments()).isEqualTo(5);
		assertThat(response.summary().scheduled()).isEqualTo(1);
		assertThat(response.summary().inProgress()).isEqualTo(1);
		assertThat(response.summary().completed()).isEqualTo(1);
		assertThat(response.summary().cancelled()).isEqualTo(1);
		assertThat(response.summary().noShow()).isEqualTo(1);
		assertThat(response.summary().occupiedMinutes()).isEqualTo(90);
		assertThat(response.nextAppointment().appointmentId()).isEqualTo(3L);
		assertThat(response.nextAppointment().status()).isEqualTo(AppointmentStatus.IN_PROGRESS);
	}

	@Test
	void returnEmptyDashboardWhenBarberHasNoAppointments() {
		GetBarberDailyDashboardService service = service();

		when(barberRepositoryPort.findById(2L)).thenReturn(Optional.of(barber()));
		when(appointmentRepositoryPort.findByBarberIdAndDate(2L, DATE)).thenReturn(List.of());

		BarberDailyDashboardResponse response = service.getDailyDashboard(command());

		assertThat(response.appointments()).isEmpty();
		assertThat(response.nextAppointment()).isNull();
		assertThat(response.summary().totalAppointments()).isZero();
		assertThat(response.summary().occupiedMinutes()).isZero();
		verify(customerRepositoryPort, never()).findById(1L);
		verify(serviceOfferingRepositoryPort, never()).findById(3L);
	}

	@Test
	void failWhenBarberDoesNotExist() {
		GetBarberDailyDashboardService service = service();

		when(barberRepositoryPort.findById(2L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getDailyDashboard(command()))
				.isInstanceOf(BarberNotFoundException.class)
				.hasMessage("Barber not found");
	}

	@Test
	void failWhenAppointmentCustomerDoesNotExist() {
		GetBarberDailyDashboardService service = service();

		when(barberRepositoryPort.findById(2L)).thenReturn(Optional.of(barber()));
		when(appointmentRepositoryPort.findByBarberIdAndDate(2L, DATE))
				.thenReturn(List.of(appointment(1L, 8, AppointmentStatus.SCHEDULED)));
		when(customerRepositoryPort.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getDailyDashboard(command()))
				.isInstanceOf(CustomerNotFoundException.class)
				.hasMessage("Customer not found");
	}

	private GetBarberDailyDashboardService service() {
		return new GetBarberDailyDashboardService(
				barberRepositoryPort,
				appointmentRepositoryPort,
				customerRepositoryPort,
				serviceOfferingRepositoryPort,
				CLOCK
		);
	}

	private GetBarberDailyDashboardCommand command() {
		return new GetBarberDailyDashboardCommand(2L, DATE);
	}

	private Barber barber() {
		return new Barber(2L, "Carlos Gomez", "3101234567", "carlos@example.com", true, CREATED_AT, UPDATED_AT);
	}

	private Customer customer(Long id, String fullName) {
		return new Customer(id, fullName, "3001234567", "customer" + id + "@example.com", true, CREATED_AT, UPDATED_AT);
	}

	private ServiceOffering serviceOffering() {
		return new ServiceOffering(
				3L,
				"Corte clasico",
				"Corte tradicional",
				30,
				new BigDecimal("25000.00"),
				true,
				CREATED_AT,
				UPDATED_AT
		);
	}

	private Appointment appointment(Long id, int hour, AppointmentStatus status) {
		return new Appointment(
				id,
				id,
				2L,
				3L,
				LocalDateTime.of(DATE.getYear(), DATE.getMonth(), DATE.getDayOfMonth(), hour, 0),
				LocalDateTime.of(DATE.getYear(), DATE.getMonth(), DATE.getDayOfMonth(), hour, 30),
				status,
				AppointmentSource.ONLINE,
				CREATED_AT,
				null
		);
	}
}
