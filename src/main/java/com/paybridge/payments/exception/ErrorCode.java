package com.paybridge.payments.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	
	UNEXPECTED_ERROR(30000, HttpStatus.INTERNAL_SERVER_ERROR, false), 
	RAZORPAY_BAD_REQUEST(30001, HttpStatus.BAD_REQUEST, false), 
	RAZORPAY_UNAUTHORIZED(30002, HttpStatus.UNAUTHORIZED, false), 
	RAZORPAY_ORDER_CREATION_FAILED(30003, HttpStatus.UNPROCESSABLE_ENTITY, false), 
	RAZORPAY_RATE_LIMITED(30004, HttpStatus.TOO_MANY_REQUESTS, true), 
	RAZORPAY_SERVER_ERROR(30005, HttpStatus.INTERNAL_SERVER_ERROR, true),
	RAZORPAY_VALIDATION_FAILED(30006, HttpStatus.BAD_REQUEST, false),
	RAZORPAY_UNREACHABLE(30007, HttpStatus.SERVICE_UNAVAILABLE, true),
	RAZORPAY_TIMEOUT(30008, HttpStatus.GATEWAY_TIMEOUT, true),
	CIRCUIT_BREAKER_OPEN(30009, HttpStatus.SERVICE_UNAVAILABLE, true),
	RAZORPAY_CLIENT_ERROR(30010, HttpStatus.BAD_GATEWAY, true);

	private final int code;
    private final HttpStatus httpStatus;
    private final boolean retryable;
	
	ErrorCode(int code, HttpStatus httpStatus, boolean retryable) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.retryable = retryable;
    }
	
	public int getCode() { return code; }
    public HttpStatus getHttpStatus() { return httpStatus; }
    public boolean isRetryable() { return retryable; }
}
