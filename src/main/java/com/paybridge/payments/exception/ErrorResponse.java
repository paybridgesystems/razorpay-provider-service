package com.paybridge.payments.exception;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

	private int errorCode;

	private String message;

	private String traceId;

	private Instant timestamp;

	private String path;

	private boolean retryable;

	private Map<String, Object> details;

}
