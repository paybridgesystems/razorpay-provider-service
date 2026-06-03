package com.paybridge.payments.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paybridge.payments.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/internal/payments")
public class PaymentController {
	private final PaymentService paymentService;

	@PostMapping("/capture")
	public void capturePayment() {
		log.info("capturePayment called");
		paymentService.capturePayment();
	}

	@GetMapping
	public ResponseEntity<String> health() {
		log.info("Health check for ProviderController");
		return ResponseEntity.ok(paymentService.capturePayment());
	}
}
