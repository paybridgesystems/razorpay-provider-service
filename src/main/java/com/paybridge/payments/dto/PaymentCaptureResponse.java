package com.paybridge.payments.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCaptureResponse {
	
	private String razorpayPaymentId;
	
	private String razorpayOrderId;
	
	private String status;
	
	private int amount;
	
	private String currency;
}