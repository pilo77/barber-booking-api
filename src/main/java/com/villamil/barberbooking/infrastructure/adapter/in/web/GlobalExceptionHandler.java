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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import jakarta.servlet.http.HttpServletRequest;

import com.villamil.barberbooking.domain.exception.AppointmentInvalidStatusTransitionException;
import com.villamil.barberbooking.domain.exception.AppointmentNotAvailableException;
import com.villamil.barberbooking.domain.exception.AppointmentNotFoundException;
import com.villamil.barberbooking.domain.exception.AppointmentOutsideWorkingHoursException;
import com.villamil.barberbooking.domain.exception.AuthenticationFailedException;
import com.villamil.barberbooking.domain.exception.BarberAlreadyExistsException;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;
import com.villamil.barberbooking.domain.exception.BarberWorkingHourNotFoundException;
import com.villamil.barberbooking.domain.exception.BarberWorkingHourOverlapException;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.exception.CustomerAlreadyExistsException;
import com.villamil.barberbooking.domain.exception.CustomerNotFoundException;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;
import com.villamil.barberbooking.domain.exception.ResourceInactiveException;
import com.villamil.barberbooking.domain.exception.ServiceOfferingAlreadyExistsException;
import com.villamil.barberbooking.domain.exception.ServiceOfferingNotFoundException;
import com.villamil.barberbooking.domain.exception.UserAccountAlreadyExistsException;
import com.villamil.barberbooking.domain.exception.UserAccountNotFoundException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AppointmentNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleAppointmentNotFound(AppointmentNotFoundException exception, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "Appointment not found", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(AuthenticationFailedException.class)
	public ResponseEntity<ProblemDetail> handleAuthenticationFailed(AuthenticationFailedException exception, HttpServletRequest request) {
		return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(ForbiddenOperationException.class)
	public ResponseEntity<ProblemDetail> handleForbidden(ForbiddenOperationException exception, HttpServletRequest request) {
		return problem(HttpStatus.FORBIDDEN, "Forbidden", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(AppointmentNotAvailableException.class)
	public ResponseEntity<ProblemDetail> handleAppointmentNotAvailable(AppointmentNotAvailableException exception, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "Appointment not available", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(AppointmentOutsideWorkingHoursException.class)
	public ResponseEntity<ProblemDetail> handleAppointmentOutsideWorkingHours(
			AppointmentOutsideWorkingHoursException exception,
			HttpServletRequest request
	) {
		return problem(HttpStatus.CONFLICT, "Appointment outside working hours", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(AppointmentInvalidStatusTransitionException.class)
	public ResponseEntity<ProblemDetail> handleAppointmentInvalidStatusTransition(
			AppointmentInvalidStatusTransitionException exception,
			HttpServletRequest request
	) {
		return problem(HttpStatus.CONFLICT, "Invalid appointment transition", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(CustomerNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleCustomerNotFound(CustomerNotFoundException exception, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "Customer not found", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(CustomerAlreadyExistsException.class)
	public ResponseEntity<ProblemDetail> handleCustomerAlreadyExists(CustomerAlreadyExistsException exception, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "Customer already exists", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(BarberNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleBarberNotFound(BarberNotFoundException exception, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "Barber not found", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(BarberAlreadyExistsException.class)
	public ResponseEntity<ProblemDetail> handleBarberAlreadyExists(BarberAlreadyExistsException exception, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "Barber already exists", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(BarberWorkingHourNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleBarberWorkingHourNotFound(
			BarberWorkingHourNotFoundException exception,
			HttpServletRequest request
	) {
		return problem(HttpStatus.NOT_FOUND, "Working hour not found", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(BarberWorkingHourOverlapException.class)
	public ResponseEntity<ProblemDetail> handleBarberWorkingHourOverlap(
			BarberWorkingHourOverlapException exception,
			HttpServletRequest request
	) {
		return problem(HttpStatus.CONFLICT, "Working hour overlap", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(ServiceOfferingNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleServiceOfferingNotFound(ServiceOfferingNotFoundException exception, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "Service offering not found", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(ServiceOfferingAlreadyExistsException.class)
	public ResponseEntity<ProblemDetail> handleServiceOfferingAlreadyExists(
			ServiceOfferingAlreadyExistsException exception,
			HttpServletRequest request
	) {
		return problem(HttpStatus.CONFLICT, "Service offering already exists", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(UserAccountNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleUserAccountNotFound(UserAccountNotFoundException exception, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "User account not found", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(UserAccountAlreadyExistsException.class)
	public ResponseEntity<ProblemDetail> handleUserAccountAlreadyExists(UserAccountAlreadyExistsException exception, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "User account already exists", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(ResourceInactiveException.class)
	public ResponseEntity<ProblemDetail> handleResourceInactive(ResourceInactiveException exception, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "Resource inactive", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<ProblemDetail> handleBusinessRule(BusinessRuleException exception, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(), request, exception);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		String detail = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.findFirst()
				.map(DefaultMessageSourceResolvable::getDefaultMessage)
				.orElse("Request validation failed");
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", detail, request, exception);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", "Request validation failed", request, exception);
	}

	@ExceptionHandler({
			MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class
	})
	public ResponseEntity<ProblemDetail> handleInvalidRequestParameter(Exception exception, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", "Request validation failed", request, exception);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ProblemDetail> handleDataIntegrity(HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "Data conflict", "Request conflicts with existing data", request, null);
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail, HttpServletRequest request, Exception exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
		problemDetail.setTitle(title);
		// Standard fields for clients
		problemDetail.setProperty("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
		problemDetail.setProperty("status", status.value());
		problemDetail.setProperty("error", status.getReasonPhrase());
		problemDetail.setProperty("message", detail);
		if (request != null) {
			problemDetail.setProperty("path", request.getRequestURI());
		}
		// Derive a stable code from the exception class name when available
		if (exception != null) {
			String simple = exception.getClass().getSimpleName().replaceAll("Exception$", "");
			String code = simple.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
			problemDetail.setProperty("code", code);
		}
		return ResponseEntity.status(status).body(problemDetail);
	}
}
