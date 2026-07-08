package com.paybridge.payments.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paybridge.payments.service.WebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/internal/payments")
public class WebhookController {

	private final WebhookService webhookService;

	@PostMapping("/webhook")
	public ResponseEntity<Void> handleWebhook(
			@RequestBody String rawPayload,
			@RequestHeader("X-Razorpay-Signature") String razorpaySignature) {

		log.info("Webhook POST received");
		webhookService.handleWebhook(rawPayload, razorpaySignature);
		return ResponseEntity.ok().build();
	}
}