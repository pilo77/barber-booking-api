package com.villamil.barberbooking.application.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.villamil.barberbooking.domain.model.Appointment;

public interface AppointmentRepositoryPort {

	Appointment save(Appointment appointment);

	Optional<Appointment> findById(Long id);

	boolean existsBlockingOverlap(Long barberId, LocalDateTime startAt, LocalDateTime endAt);

	List<Appointment> findByBarberIdAndDate(Long barberId, LocalDate date);
}
