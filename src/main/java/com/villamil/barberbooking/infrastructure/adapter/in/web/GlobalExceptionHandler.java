package com.villamil.barberbooking.infrastructure.adapter.in.web;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.villamil.barberbooking.domain.exception.AppointmentInvalidStatusTransitionException;
import com.villamil.barberbooking.domain.exception.AppointmentNotAvailableException;
import com.villamil.barberbooking.domain.exception.AppointmentNotFoundException;
import com.villamil.barberbooking.domain.exception.AppointmentOutsideWorkingHoursException;
import com.villamil.barberbooking.domain.exception.BarberAlreadyExistsException;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;
import com.villamil.barberbooking.domain.exception.BarberWorkingHourNotFoundException;
import com.villamil.barberbooking.domain.exception.BarberWorkingHourOverlapException;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.exception.CustomerAlreadyExistsException;
import com.villamil.barberbooking.domain.exception.CustomerNotFoundException;
import com.villamil.barberbooking.domain.exception.ResourceInactiveException;
import com.villamil.barberbooking.domain.exception.ServiceOfferingAlreadyExistsException;
import com.villamil.barberbooking.domain.exception.ServiceOfferingNotFoundException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AppointmentNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleAppointmentNotFound(AppointmentNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Appointment not found", exception.getMessage());
	}

	@ExceptionHandler(AppointmentNotAvailableException.class)
	public ResponseEntity<ProblemDetail> handleAppointmentNotAvailable(AppointmentNotAvailableException exception) {
		return problem(HttpStatus.CONFLICT, "Appointment not available", exception.getMessage());
	}

	@ExceptionHandler(AppointmentOutsideWorkingHoursException.class)
	public ResponseEntity<ProblemDetail> handleAppointmentOutsideWorkingHours(
			AppointmentOutsideWorkingHoursException exception
	) {
		return problem(HttpStatus.CONFLICT, "Appointment outside working hours", exception.getMessage());
	}

	@ExceptionHandler(AppointmentInvalidStatusTransitionException.class)
	public ResponseEntity<ProblemDetail> handleAppointmentInvalidStatusTransition(
			AppointmentInvalidStatusTransitionException exception
	) {
		return problem(HttpStatus.CONFLICT, "Invalid appointment transition", exception.getMessage());
	}

	@ExceptionHandler(CustomerNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleCustomerNotFound(CustomerNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Customer not found", exception.getMessage());
	}

	@ExceptionHandler(CustomerAlreadyExistsException.class)
	public ResponseEntity<ProblemDetail> handleCustomerAlreadyExists(CustomerAlreadyExistsException exception) {
		return problem(HttpStatus.CONFLICT, "Customer already exists", exception.getMessage());
	}

	@ExceptionHandler(BarberNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleBarberNotFound(BarberNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Barber not found", exception.getMessage());
	}

	@ExceptionHandler(BarberAlreadyExistsException.class)
	public ResponseEntity<ProblemDetail> handleBarberAlreadyExists(BarberAlreadyExistsException exception) {
		return problem(HttpStatus.CONFLICT, "Barber already exists", exception.getMessage());
	}

	@ExceptionHandler(BarberWorkingHourNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleBarberWorkingHourNotFound(
			BarberWorkingHourNotFoundException exception
	) {
		return problem(HttpStatus.NOT_FOUND, "Working hour not found", exception.getMessage());
	}

	@ExceptionHandler(BarberWorkingHourOverlapException.class)
	public ResponseEntity<ProblemDetail> handleBarberWorkingHourOverlap(
			BarberWorkingHourOverlapException exception
	) {
		return problem(HttpStatus.CONFLICT, "Working hour overlap", exception.getMessage());
	}

	@ExceptionHandler(ServiceOfferingNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleServiceOfferingNotFound(ServiceOfferingNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Service offering not found", exception.getMessage());
	}

	@ExceptionHandler(ServiceOfferingAlreadyExistsException.class)
	public ResponseEntity<ProblemDetail> handleServiceOfferingAlreadyExists(
			ServiceOfferingAlreadyExistsException exception
	) {
		return problem(HttpStatus.CONFLICT, "Service offering already exists", exception.getMessage());
	}

	@ExceptionHandler(ResourceInactiveException.class)
	public ResponseEntity<ProblemDetail> handleResourceInactive(ResourceInactiveException exception) {
		return problem(HttpStatus.CONFLICT, "Resource inactive", exception.getMessage());
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<ProblemDetail> handleBusinessRule(BusinessRuleException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
		String detail = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.findFirst()
				.map(DefaultMessageSourceResolvable::getDefaultMessage)
				.orElse("Request validation failed");
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", detail);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", "Request validation failed");
	}

	@ExceptionHandler({
			MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class
	})
	public ResponseEntity<ProblemDetail> handleInvalidRequestParameter(Exception exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", "Request validation failed");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ProblemDetail> handleDataIntegrity() {
		return problem(HttpStatus.CONFLICT, "Data conflict", "Request conflicts with existing data");
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
		problemDetail.setTitle(title);
		return ResponseEntity.status(status).body(problemDetail);
	}
}
