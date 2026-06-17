package com.villamil.barberbooking.infrastructure.adapter.in.web;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.exception.CustomerAlreadyExistsException;
import com.villamil.barberbooking.domain.exception.CustomerNotFoundException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomerNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleCustomerNotFound(CustomerNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Customer not found", exception.getMessage());
	}

	@ExceptionHandler(CustomerAlreadyExistsException.class)
	public ResponseEntity<ProblemDetail> handleCustomerAlreadyExists(CustomerAlreadyExistsException exception) {
		return problem(HttpStatus.CONFLICT, "Customer already exists", exception.getMessage());
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<ProblemDetail> handleBusinessRule(BusinessRuleException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid business request", exception.getMessage());
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
