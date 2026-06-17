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

import com.villamil.barberbooking.application.dto.command.CreateCustomerCommand;
import com.villamil.barberbooking.application.dto.command.UpdateCustomerCommand;
import com.villamil.barberbooking.application.dto.response.CustomerResponse;
import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.exception.CustomerAlreadyExistsException;
import com.villamil.barberbooking.domain.exception.CustomerNotFoundException;
import com.villamil.barberbooking.domain.model.Customer;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

	private static final Instant CREATED_AT = Instant.parse("2026-06-17T12:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-06-17T12:30:00Z");

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	private CustomerUniquenessValidator customerUniquenessValidator;

	@BeforeEach
	void setUp() {
		customerUniquenessValidator = new CustomerUniquenessValidator(customerRepositoryPort);
	}

	@Test
	void createCustomerSuccessfully() {
		CreateCustomerService service = new CreateCustomerService(
				customerRepositoryPort,
				customerUniquenessValidator
		);
		CreateCustomerCommand command = new CreateCustomerCommand("Ana Perez", "3001234567", "ana@example.com");

		when(customerRepositoryPort.existsByPhone("3001234567")).thenReturn(false);
		when(customerRepositoryPort.existsByEmail("ana@example.com")).thenReturn(false);
		when(customerRepositoryPort.save(any(Customer.class))).thenReturn(customer());

		CustomerResponse response = service.create(command);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.fullName()).isEqualTo("Ana Perez");
		assertThat(response.active()).isTrue();
	}

	@Test
	void rejectCustomerWithDuplicatedPhone() {
		CreateCustomerService service = new CreateCustomerService(
				customerRepositoryPort,
				customerUniquenessValidator
		);
		CreateCustomerCommand command = new CreateCustomerCommand("Ana Perez", "3001234567", null);

		when(customerRepositoryPort.existsByPhone("3001234567")).thenReturn(true);

		assertThatThrownBy(() -> service.create(command))
				.isInstanceOf(CustomerAlreadyExistsException.class)
				.hasMessage("Customer phone already exists");
		verify(customerRepositoryPort, never()).save(any(Customer.class));
	}

	@Test
	void rejectCustomerWithBlankName() {
		CreateCustomerService service = new CreateCustomerService(
				customerRepositoryPort,
				customerUniquenessValidator
		);
		CreateCustomerCommand command = new CreateCustomerCommand(" ", "3001234567", null);

		assertThatThrownBy(() -> service.create(command))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Customer full name is required");
	}

	@Test
	void getExistingCustomer() {
		GetCustomerService service = new GetCustomerService(customerRepositoryPort);

		when(customerRepositoryPort.findById(1L)).thenReturn(Optional.of(customer()));

		CustomerResponse response = service.getById(1L);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.phone()).isEqualTo("3001234567");
	}

	@Test
	void failWhenCustomerDoesNotExist() {
		GetCustomerService service = new GetCustomerService(customerRepositoryPort);

		when(customerRepositoryPort.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(99L))
				.isInstanceOf(CustomerNotFoundException.class)
				.hasMessage("Customer not found");
	}

	@Test
	void listCustomers() {
		ListCustomersService service = new ListCustomersService(customerRepositoryPort);

		when(customerRepositoryPort.findAll()).thenReturn(List.of(customer()));

		List<CustomerResponse> customers = service.list();

		assertThat(customers).hasSize(1);
		assertThat(customers.getFirst().id()).isEqualTo(1L);
	}

	@Test
	void updateCustomerSuccessfully() {
		UpdateCustomerService service = new UpdateCustomerService(
				customerRepositoryPort,
				customerUniquenessValidator
		);
		UpdateCustomerCommand command = new UpdateCustomerCommand("Ana Maria Perez", "3009876543", null);

		when(customerRepositoryPort.findById(1L)).thenReturn(Optional.of(customer()));
		when(customerRepositoryPort.existsByPhoneAndIdNot("3009876543", 1L)).thenReturn(false);
		when(customerRepositoryPort.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CustomerResponse response = service.update(1L, command);

		assertThat(response.fullName()).isEqualTo("Ana Maria Perez");
		assertThat(response.phone()).isEqualTo("3009876543");
		assertThat(response.active()).isTrue();
	}

	@Test
	void rejectUpdateWithDuplicatedPhone() {
		UpdateCustomerService service = new UpdateCustomerService(
				customerRepositoryPort,
				customerUniquenessValidator
		);
		UpdateCustomerCommand command = new UpdateCustomerCommand("Ana Perez", "3009876543", null);

		when(customerRepositoryPort.findById(1L)).thenReturn(Optional.of(customer()));
		when(customerRepositoryPort.existsByPhoneAndIdNot("3009876543", 1L)).thenReturn(true);

		assertThatThrownBy(() -> service.update(1L, command))
				.isInstanceOf(CustomerAlreadyExistsException.class)
				.hasMessage("Customer phone already exists");
		verify(customerRepositoryPort, never()).save(any(Customer.class));
	}

	@Test
	void deactivateCustomer() {
		DeactivateCustomerService service = new DeactivateCustomerService(customerRepositoryPort);

		when(customerRepositoryPort.findById(1L)).thenReturn(Optional.of(customer()));
		when(customerRepositoryPort.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CustomerResponse response = service.deactivate(1L);

		assertThat(response.active()).isFalse();
	}

	private Customer customer() {
		return new Customer(
				1L,
				"Ana Perez",
				"3001234567",
				"ana@example.com",
				true,
				CREATED_AT,
				UPDATED_AT
		);
	}
}
