package com.paybridge.payments.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	
	GENERAL_ERROR(30000, HttpStatus.INTERNAL_SERVER_ERROR, false), 
	INVALID_REQUEST(30001, HttpStatus.BAD_REQUEST, false), 
	INTERNAL_ERROR(30002, HttpStatus.INTERNAL_SERVER_ERROR, false);
	
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
