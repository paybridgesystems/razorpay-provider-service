package com.paybridge.payments.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.paybridge.payments.exception.resolver.ErrorMessageResolver;
import com.paybridge.payments.constant.RazorpayConstants;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private final ErrorMessageResolver messageResolver;

	public GlobalExceptionHandler(ErrorMessageResolver messageResolver) {
		this.messageResolver = messageResolver;
	}

	// --- Handles your custom domain exception ---

	@ExceptionHandler(RazorpayProviderException.class)
	public ResponseEntity<ErrorResponse> handleRazorpayProviderException(
			RazorpayProviderException ex,
			HttpServletRequest request) {

		ErrorCode errorCode = ex.getErrorCode();

		ErrorResponse response = ErrorResponse.builder()
				.errorCode(errorCode.getCode())
				.message(messageResolver.resolve(errorCode))
				.traceId(MDC.get(RazorpayConstants.TRACE_ID))
				.timestamp(Instant.now())
				.path(request.getRequestURI())
				.retryable(errorCode.isRetryable())
				.details(ex.getDetails().isEmpty() ? null : ex.getDetails())
				.build();

		return ResponseEntity
				.status(errorCode.getHttpStatus())
				.body(response);
	}

	// --- Handles JSR-380 @Valid failures ---

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {

		List<String> errors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.toList();

		ErrorResponse response = ErrorResponse.builder()
				.errorCode(ErrorCode.UNEXPECTED_ERROR.getCode())
				.message(messageResolver.resolve(ErrorCode.UNEXPECTED_ERROR))
				.traceId(MDC.get(RazorpayConstants.TRACE_ID))
				.timestamp(Instant.now())
				.path(request.getRequestURI())
				.retryable(false)
				.details(Map.of("validationErrors", errors))
				.build();

		return ResponseEntity
				.status(ErrorCode.UNEXPECTED_ERROR.getHttpStatus())
				.body(response);
	}

	// --- Safety net — catches anything you haven't explicitly handled ---

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(
			Exception ex,
			HttpServletRequest request) {

		ErrorResponse response = ErrorResponse.builder()
				.errorCode(ErrorCode.UNEXPECTED_ERROR.getCode())
				.message(messageResolver.resolve(ErrorCode.UNEXPECTED_ERROR))
				.traceId(MDC.get(RazorpayConstants.TRACE_ID))
				.timestamp(Instant.now())
				.path(request.getRequestURI())
				.retryable(false)
				.build();

		return ResponseEntity
				.status(ErrorCode.UNEXPECTED_ERROR.getHttpStatus())
				.body(response);
	}
}