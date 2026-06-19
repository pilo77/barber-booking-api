package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.villamil.barberbooking.application.dto.command.CreatePublicAppointmentCommand;
import com.villamil.barberbooking.application.dto.command.PublicCustomerCommand;
import com.villamil.barberbooking.application.dto.response.BarberAvailabilityResponse;
import com.villamil.barberbooking.application.dto.response.PublicAppointmentResponse;
import com.villamil.barberbooking.application.dto.response.PublicBarberResponse;
import com.villamil.barberbooking.application.dto.response.PublicServiceOfferingResponse;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.application.port.out.PublicBarberShopRepositoryPort;
import com.villamil.barberbooking.application.port.out.TenantContextExecutor;
import com.villamil.barberbooking.application.tenant.PublicTenantContext;
import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.domain.exception.AppointmentNotAvailableException;
import com.villamil.barberbooking.domain.exception.AppointmentOutsideWorkingHoursException;
import com.villamil.barberbooking.domain.exception.PublicResourceNotFoundException;
import com.villamil.barberbooking.domain.model.Appointment;
import com.villamil.barberbooking.domain.model.Customer;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

@ExtendWith(MockitoExtension.class)
class PublicBookingServiceTest {

	private static final LocalDateTime START_AT = LocalDateTime.of(2026, 6, 20, 10, 0);
	private static final PublicTenantContext PUBLIC_TENANT = new PublicTenantContext(7L, 9L);

	@Mock
	private PublicBarberShopRepositoryPort publicBarberShopRepositoryPort;

	@Mock
	private TenantContextExecutor tenantContextExecutor;

	@Mock
	private BarberAvailabilityCalculator barberAvailabilityCalculator;

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@Mock
	private AppointmentBookingPolicy appointmentBookingPolicy;

	@Mock
	private AppointmentRepositoryPort appointmentRepositoryPort;

	private PublicBookingService service;

	@BeforeEach
	void setUp() {
		service = new PublicBookingService(
				publicBarberShopRepositoryPort,
				tenantContextExecutor,
				barberAvailabilityCalculator,
				customerRepositoryPort,
				appointmentBookingPolicy,
				appointmentRepositoryPort
		);
	}

	@Test
	void publicAvailabilityUsesTenantResolvedOnlyFromSlugs() {
		LocalDate date = LocalDate.of(2026, 6, 20);
		BarberAvailabilityResponse expected = new BarberAvailabilityResponse(2L, 1L, date, List.of());
		executeTenantActions();
		mockPublicResources();
		when(barberAvailabilityCalculator.calculate(any())).thenReturn(expected);

		BarberAvailabilityResponse response = service.getAvailability(
				"Ponte-Perro",
				"Neiva-Centro",
				2L,
				1L,
				date
		);

		assertThat(response).isEqualTo(expected);
		verify(publicBarberShopRepositoryPort).findActiveTenantBySlugs("ponte-perro", "neiva-centro");
		verify(tenantContextExecutor).withTenant(eq(new TenantContext(7L, 9L)), any());
	}

	@Test
	void invisibleBarberCannotExposeAvailability() {
		when(publicBarberShopRepositoryPort.findActiveTenantBySlugs("ponte-perro", "neiva-centro"))
				.thenReturn(Optional.of(PUBLIC_TENANT));
		when(publicBarberShopRepositoryPort.findVisibleServiceByCompanyId(7L, 1L))
				.thenReturn(Optional.of(publicService()));
		when(publicBarberShopRepositoryPort.findVisibleBarberByTenant(7L, 9L, 2L))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getAvailability(
				"ponte-perro", "neiva-centro", 2L, 1L, LocalDate.of(2026, 6, 20)))
				.isInstanceOf(PublicResourceNotFoundException.class)
				.hasMessage("Public barber not found");
		verify(tenantContextExecutor, never()).withTenant(any(), any());
	}

	@Test
	void branchFromAnotherCompanyIsNotPubliclyResolved() {
		when(publicBarberShopRepositoryPort.findActiveTenantBySlugs("ponte-perro", "other-branch"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getAvailability(
				"ponte-perro", "other-branch", 2L, 1L, LocalDate.of(2026, 6, 20)))
				.isInstanceOf(PublicResourceNotFoundException.class)
				.hasMessage("Public branch not found");
	}

	@Test
	void publicBookingReusesCustomerAndCreatesScheduledOnlineAppointment() {
		Customer customer = customer(5L, "Stored Private Name");
		Appointment unsaved = appointment(null);
		Appointment saved = appointment(10L);
		executeTenantActions();
		mockPublicResources();
		when(customerRepositoryPort.findByPhone("3001234567")).thenReturn(Optional.of(customer));
		when(appointmentBookingPolicy.createValidatedAppointment(
				5L, 2L, 1L, START_AT, AppointmentSource.ONLINE, AppointmentStatus.SCHEDULED))
				.thenReturn(unsaved);
		when(appointmentRepositoryPort.save(unsaved)).thenReturn(saved);

		PublicAppointmentResponse response = service.create(command());

		assertThat(response.id()).isEqualTo(10L);
		assertThat(response.source()).isEqualTo(AppointmentSource.ONLINE);
		assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
		assertThat(response.endAt()).isEqualTo(START_AT.plusMinutes(30));
		assertThat(response.customer().phone()).isEqualTo("3001234567");
		assertThat(response.customer().fullName()).isEqualTo("Carlos Villamil");
		verify(customerRepositoryPort, never()).save(any());
	}

	@Test
	void publicBookingCreatesCustomerInsideResolvedCompanyWhenPhoneIsNew() {
		Customer savedCustomer = customer(5L, "Carlos Villamil");
		Appointment unsaved = appointment(null);
		executeTenantActions();
		mockPublicResources();
		when(customerRepositoryPort.findByPhone("3001234567")).thenReturn(Optional.empty());
		when(customerRepositoryPort.save(any(Customer.class))).thenReturn(savedCustomer);
		when(appointmentBookingPolicy.createValidatedAppointment(
				5L, 2L, 1L, START_AT, AppointmentSource.ONLINE, AppointmentStatus.SCHEDULED))
				.thenReturn(unsaved);
		when(appointmentRepositoryPort.save(unsaved)).thenReturn(appointment(10L));

		service.create(command());

		verify(customerRepositoryPort).save(any(Customer.class));
		verify(tenantContextExecutor).withTenant(any(TenantContext.class), any());
	}

	@Test
	void invisibleServiceCannotBeBooked() {
		when(publicBarberShopRepositoryPort.findActiveTenantBySlugs("ponte-perro", "neiva-centro"))
				.thenReturn(Optional.of(PUBLIC_TENANT));
		when(publicBarberShopRepositoryPort.findVisibleServiceByCompanyId(7L, 1L))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(command()))
				.isInstanceOf(PublicResourceNotFoundException.class)
				.hasMessage("Public service offering not found");
	}

	@Test
	void barberFromAnotherBranchCannotBeBooked() {
		when(publicBarberShopRepositoryPort.findActiveTenantBySlugs("ponte-perro", "neiva-centro"))
				.thenReturn(Optional.of(PUBLIC_TENANT));
		when(publicBarberShopRepositoryPort.findVisibleServiceByCompanyId(7L, 1L))
				.thenReturn(Optional.of(publicService()));
		when(publicBarberShopRepositoryPort.findVisibleBarberByTenant(7L, 9L, 2L))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(command()))
				.isInstanceOf(PublicResourceNotFoundException.class)
				.hasMessage("Public barber not found");
	}

	@Test
	void workingHoursAndOverlapFailuresArePreservedForPublicBooking() {
		Customer customer = customer(5L, "Carlos Villamil");
		executeTenantActions();
		mockPublicResources();
		when(customerRepositoryPort.findByPhone("3001234567")).thenReturn(Optional.of(customer));
		when(appointmentBookingPolicy.createValidatedAppointment(
				5L, 2L, 1L, START_AT, AppointmentSource.ONLINE, AppointmentStatus.SCHEDULED))
				.thenThrow(new AppointmentOutsideWorkingHoursException("Appointment is outside barber working hours"));

		assertThatThrownBy(() -> service.create(command()))
				.isInstanceOf(AppointmentOutsideWorkingHoursException.class);

		doThrow(new AppointmentNotAvailableException("Appointment overlaps with an active appointment"))
				.when(appointmentBookingPolicy).createValidatedAppointment(
						5L, 2L, 1L, START_AT, AppointmentSource.ONLINE, AppointmentStatus.SCHEDULED);

		assertThatThrownBy(() -> service.create(command()))
				.isInstanceOf(AppointmentNotAvailableException.class);
	}

	private void executeTenantActions() {
		when(tenantContextExecutor.withTenant(any(TenantContext.class), any()))
				.thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());
	}

	private void mockPublicResources() {
		when(publicBarberShopRepositoryPort.findActiveTenantBySlugs("ponte-perro", "neiva-centro"))
				.thenReturn(Optional.of(PUBLIC_TENANT));
		when(publicBarberShopRepositoryPort.findVisibleServiceByCompanyId(7L, 1L))
				.thenReturn(Optional.of(publicService()));
		when(publicBarberShopRepositoryPort.findVisibleBarberByTenant(7L, 9L, 2L))
				.thenReturn(Optional.of(publicBarber()));
	}

	private CreatePublicAppointmentCommand command() {
		return new CreatePublicAppointmentCommand(
				"ponte-perro",
				"neiva-centro",
				1L,
				2L,
				START_AT,
				new PublicCustomerCommand("Carlos Villamil", "3001234567", "cliente@example.com")
		);
	}

	private PublicServiceOfferingResponse publicService() {
		return new PublicServiceOfferingResponse(1L, "Corte clasico", null, 30, new BigDecimal("25000.00"));
	}

	private PublicBarberResponse publicBarber() {
		return new PublicBarberResponse(2L, "Santiago", null, null, null);
	}

	private Customer customer(Long id, String fullName) {
		return new Customer(
				id,
				fullName,
				"3001234567",
				"cliente@example.com",
				true,
				Instant.parse("2026-06-18T12:00:00Z"),
				Instant.parse("2026-06-18T12:00:00Z")
		);
	}

	private Appointment appointment(Long id) {
		return new Appointment(
				id,
				5L,
				2L,
				1L,
				START_AT,
				START_AT.plusMinutes(30),
				AppointmentStatus.SCHEDULED,
				AppointmentSource.ONLINE,
				Instant.parse("2026-06-18T12:00:00Z"),
				null
		);
	}
}
