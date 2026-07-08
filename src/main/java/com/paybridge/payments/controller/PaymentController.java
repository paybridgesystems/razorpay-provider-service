package com.paybridge.payments.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paybridge.payments.dto.PaymentCaptureRequest;
import com.paybridge.payments.dto.PaymentCaptureResponse;
import com.paybridge.payments.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/internal/payments")
public class PaymentController {

	private final PaymentService paymentService;

	@PostMapping("/verify-and-capture")
	public ResponseEntity<PaymentCaptureResponse> verifyAndCapture(@RequestBody @Valid PaymentCaptureRequest request) {
		log.info("verifyAndCapture called with request: {}", request);

		PaymentCaptureResponse response = paymentService.verifyAndCapture(request);

		log.info("Payment verified and captured successfully for orderId: {}", request.getRazorpayOrderId());
		return ResponseEntity.ok(response);
	}
}
