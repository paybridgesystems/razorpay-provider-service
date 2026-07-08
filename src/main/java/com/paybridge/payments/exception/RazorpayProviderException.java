package com.paybridge.payments.exception;

import java.io.Serial;
import java.util.Map;

import lombok.Getter;
import lombok.ToString;

@Getter	
@ToString
public class RazorpayProviderException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;	

	private final ErrorCode errorCode;
	private final Map<String, Object> details;

	public RazorpayProviderException(ErrorCode errorCode) {
		super(errorCode.name());
		this.errorCode = errorCode;
		this.details = Map.of();
	}

	public RazorpayProviderException(ErrorCode errorCode, Map<String, Object> details) {
		super(errorCode.name());
		this.errorCode = errorCode;
		this.details = details;
	}
}
