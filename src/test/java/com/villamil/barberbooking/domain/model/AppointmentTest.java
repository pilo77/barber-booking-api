package com.villamil.barberbooking.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

class AppointmentTest {

	@Test
	void createCalculatesEndTimeAndDefaultsToScheduled() {
		LocalDateTime startAt = LocalDateTime.of(2026, 6, 17, 9, 0);

		Appointment appointment = Appointment.create(1L, 2L, 3L, startAt, 30, AppointmentSource.ONLINE);

		assertThat(appointment.startAt()).isEqualTo(startAt);
		assertThat(appointment.endAt()).isEqualTo(startAt.plusMinutes(30));
		assertThat(appointment.status()).isEqualTo(AppointmentStatus.SCHEDULED);
		assertThat(appointment.blocksAvailability()).isTrue();
	}

	@Test
	void createWalkInAppointmentInProgress() {
		LocalDateTime startAt = LocalDateTime.of(2026, 6, 17, 9, 0);

		Appointment appointment = Appointment.create(
				1L,
				2L,
				3L,
				startAt,
				30,
				AppointmentSource.WALK_IN,
				AppointmentStatus.IN_PROGRESS
		);

		assertThat(appointment.status()).isEqualTo(AppointmentStatus.IN_PROGRESS);
		assertThat(appointment.source()).isEqualTo(AppointmentSource.WALK_IN);
		assertThat(appointment.blocksAvailability()).isTrue();
	}

	@Test
	void detectsOverlappingRanges() {
		Appointment appointment = Appointment.create(
				1L,
				2L,
				3L,
				LocalDateTime.of(2026, 6, 17, 9, 0),
				30,
				AppointmentSource.ONLINE
		);

		assertThat(appointment.overlaps(
				LocalDateTime.of(2026, 6, 17, 9, 15),
				LocalDateTime.of(2026, 6, 17, 9, 45)
		)).isTrue();
		assertThat(appointment.overlaps(
				LocalDateTime.of(2026, 6, 17, 9, 30),
				LocalDateTime.of(2026, 6, 17, 10, 0)
		)).isFalse();
	}

	@Test
	void rejectsInvalidDuration() {
		assertThatThrownBy(() -> Appointment.create(
				1L,
				2L,
				3L,
				LocalDateTime.of(2026, 6, 17, 9, 0),
				0,
				AppointmentSource.ONLINE
		)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void rejectsInvalidInitialStatus() {
		assertThatThrownBy(() -> Appointment.create(
				1L,
				2L,
				3L,
				LocalDateTime.of(2026, 6, 17, 9, 0),
				30,
				AppointmentSource.WALK_IN,
				AppointmentStatus.COMPLETED
		)).isInstanceOf(BusinessRuleException.class)
				.hasMessage("Appointment initial status must be scheduled or in-progress");
	}
}
