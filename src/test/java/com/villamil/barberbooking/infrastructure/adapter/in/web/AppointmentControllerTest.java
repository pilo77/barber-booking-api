package com.villamil.barberbooking.infrastructure.adapter.in.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.villamil.barberbooking.application.dto.command.BookAppointmentCommand;
import com.villamil.barberbooking.application.dto.response.AppointmentResponse;
import com.villamil.barberbooking.application.dto.response.BarberDailyScheduleResponse;
import com.villamil.barberbooking.application.port.in.BookAppointmentUseCase;
import com.villamil.barberbooking.application.port.in.CancelAppointmentUseCase;
import com.villamil.barberbooking.application.port.in.CompleteAppointmentUseCase;
import com.villamil.barberbooking.application.port.in.GetAppointmentUseCase;
import com.villamil.barberbooking.application.port.in.GetBarberDailyAppointmentsUseCase;
import com.villamil.barberbooking.application.port.in.MarkAppointmentNoShowUseCase;
import com.villamil.barberbooking.application.port.in.StartAppointmentUseCase;
import com.villamil.barberbooking.domain.exception.AppointmentInvalidStatusTransitionException;
import com.villamil.barberbooking.domain.exception.AppointmentNotAvailableException;
import com.villamil.barberbooking.domain.exception.AppointmentNotFoundException;
import com.villamil.barberbooking.domain.exception.AppointmentOutsideWorkingHoursException;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

	private static final Instant CREATED_AT = Instant.parse("2026-06-17T12:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-06-17T12:30:00Z");
	private static final LocalDateTime START_AT = LocalDateTime.of(2026, 6, 22, 9, 0);

	@Mock
	private BookAppointmentUseCase bookAppointmentUseCase;

	@Mock
	private GetAppointmentUseCase getAppointmentUseCase;

	@Mock
	private GetBarberDailyAppointmentsUseCase getBarberDailyAppointmentsUseCase;

	@Mock
	private CancelAppointmentUseCase cancelAppointmentUseCase;

	@Mock
	private StartAppointmentUseCase startAppointmentUseCase;

	@Mock
	private CompleteAppointmentUseCase completeAppointmentUseCase;

	@Mock
	private MarkAppointmentNoShowUseCase markAppointmentNoShowUseCase;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper()
				.findAndRegisterModules()
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		AppointmentController controller = new AppointmentController(
				bookAppointmentUseCase,
				getAppointmentUseCase,
				getBarberDailyAppointmentsUseCase,
				cancelAppointmentUseCase,
				startAppointmentUseCase,
				completeAppointmentUseCase,
				markAppointmentNoShowUseCase
		);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	void bookAppointmentReturnsCreated() throws Exception {
		when(bookAppointmentUseCase.book(any(BookAppointmentCommand.class))).thenReturn(appointment(AppointmentStatus.SCHEDULED));

		mockMvc.perform(post("/api/v1/appointments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": 1,
								  "barberId": 2,
								  "serviceOfferingId": 3,
								  "startAt": "2026-06-22T09:00:00"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/appointments/1"))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.serviceOfferingId").value(3))
				.andExpect(jsonPath("$.endAt").value("2026-06-22T09:30:00"))
				.andExpect(jsonPath("$.status").value("SCHEDULED"));
	}

	@Test
	void rejectInvalidBookAppointmentRequest() throws Exception {
		mockMvc.perform(post("/api/v1/appointments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": null,
								  "barberId": 2,
								  "serviceOfferingId": 3,
								  "startAt": null
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request"));

		verify(bookAppointmentUseCase, never()).book(any(BookAppointmentCommand.class));
	}

	@Test
	void occupiedAppointmentReturnsConflict() throws Exception {
		when(bookAppointmentUseCase.book(any(BookAppointmentCommand.class)))
				.thenThrow(new AppointmentNotAvailableException("Appointment overlaps with an active appointment"));

		mockMvc.perform(post("/api/v1/appointments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": 1,
								  "barberId": 2,
								  "serviceOfferingId": 3,
								  "startAt": "2026-06-22T09:00:00"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Appointment not available"));
	}

	@Test
	void outsideWorkingHoursReturnsConflict() throws Exception {
		when(bookAppointmentUseCase.book(any(BookAppointmentCommand.class)))
				.thenThrow(new AppointmentOutsideWorkingHoursException("Appointment is outside barber working hours"));

		mockMvc.perform(post("/api/v1/appointments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": 1,
								  "barberId": 2,
								  "serviceOfferingId": 3,
								  "startAt": "2026-06-22T20:00:00"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Appointment outside working hours"));
	}

	@Test
	void getAppointmentReturnsOk() throws Exception {
		when(getAppointmentUseCase.getById(1L)).thenReturn(appointment(AppointmentStatus.SCHEDULED));

		mockMvc.perform(get("/api/v1/appointments/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void getMissingAppointmentReturnsNotFound() throws Exception {
		when(getAppointmentUseCase.getById(99L)).thenThrow(new AppointmentNotFoundException("Appointment not found"));

		mockMvc.perform(get("/api/v1/appointments/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Appointment not found"));
	}

	@Test
	void getBarberDailyAppointmentsReturnsOk() throws Exception {
		when(getBarberDailyAppointmentsUseCase.getDailyAppointments(2L, LocalDate.of(2026, 6, 22)))
				.thenReturn(new BarberDailyScheduleResponse(
						2L,
						LocalDate.of(2026, 6, 22),
						List.of(appointment(AppointmentStatus.SCHEDULED))
				));

		mockMvc.perform(get("/api/v1/barbers/2/appointments?date=2026-06-22"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.barberId").value(2))
				.andExpect(jsonPath("$.appointments", hasSize(1)));
	}

	@Test
	void cancelAppointmentReturnsOk() throws Exception {
		when(cancelAppointmentUseCase.cancel(1L)).thenReturn(appointment(AppointmentStatus.CANCELLED));

		mockMvc.perform(patch("/api/v1/appointments/1/cancel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	void startAppointmentReturnsOk() throws Exception {
		when(startAppointmentUseCase.start(1L)).thenReturn(appointment(AppointmentStatus.IN_PROGRESS));

		mockMvc.perform(patch("/api/v1/appointments/1/start"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"));
	}

	@Test
	void completeAppointmentReturnsOk() throws Exception {
		when(completeAppointmentUseCase.complete(1L)).thenReturn(appointment(AppointmentStatus.COMPLETED));

		mockMvc.perform(patch("/api/v1/appointments/1/complete"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	@Test
	void markAppointmentNoShowReturnsOk() throws Exception {
		when(markAppointmentNoShowUseCase.markNoShow(1L)).thenReturn(appointment(AppointmentStatus.NO_SHOW));

		mockMvc.perform(patch("/api/v1/appointments/1/no-show"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("NO_SHOW"));
	}

	@Test
	void invalidAppointmentTransitionReturnsConflict() throws Exception {
		when(completeAppointmentUseCase.complete(1L))
				.thenThrow(new AppointmentInvalidStatusTransitionException(
						"Only in-progress appointments can be completed"
				));

		mockMvc.perform(patch("/api/v1/appointments/1/complete"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Invalid appointment transition"));
	}

	@Test
	void lifecycleMissingAppointmentReturnsNotFound() throws Exception {
		when(startAppointmentUseCase.start(99L)).thenThrow(new AppointmentNotFoundException("Appointment not found"));

		mockMvc.perform(patch("/api/v1/appointments/99/start"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Appointment not found"));
	}

	private AppointmentResponse appointment(AppointmentStatus status) {
		return new AppointmentResponse(
				1L,
				1L,
				2L,
				3L,
				START_AT,
				START_AT.plusMinutes(30),
				status,
				AppointmentSource.ONLINE,
				CREATED_AT,
				status == AppointmentStatus.CANCELLED ? UPDATED_AT : null
		);
	}
}
