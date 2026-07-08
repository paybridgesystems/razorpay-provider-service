package com.paybridge.payments.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.payments.exception.ErrorCode;
import com.paybridge.payments.exception.RazorpayProviderException;
import com.paybridge.payments.repository.RazorpayOrderRepository;
import com.paybridge.payments.repository.RazorpayPaymentEventRepository;
import com.paybridge.payments.repository.entity.RazorpayPaymentEventEntity;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

	private final HmacSignatureVerifier signatureVerifier;
	private final RazorpayOrderRepository orderRepository;
	private final RazorpayPaymentEventRepository eventRepository;
	private final ObjectMapper objectMapper;

	@Value("${razorpay.webhook.secret}")
	private String webhookSecret;

	public void handleWebhook(String rawPayload, String razorpaySignature) {
		log.info("Webhook received, verifying signature");
		verifyWebhookSignature(rawPayload, razorpaySignature);

		WebhookPayload payload = parsePayload(rawPayload);
		log.info("Webhook event: {}", payload.getEventType());

		boolean isNew = eventRepository.insertIfNotExists(
				RazorpayPaymentEventEntity.builder()
				.razorpayOrderId(payload.getRazorpayOrderId())
				.razorpayPaymentId(payload.getRazorpayPaymentId())
				.eventType(payload.getEventType())
				.rawPayload(rawPayload)
				.razorpayEventId(payload.getRazorpayEventId())
				.build()
				);

		if (!isNew) {
			log.info("Duplicate webhook event received, skipping: {}",
					payload.getRazorpayEventId());
			return;  // return cleanly - WebhookController still returns 200 OK
		}

		processEvent(payload);

		eventRepository.markProcessed(payload.getRazorpayEventId());
	}

	private void processEvent(WebhookPayload payload) {
		switch (payload.getEventType()) {
		case "payment.captured" -> {
			int rows = orderRepository.updateToCaptured(
					payload.getRazorpayOrderId());
			if (rows > 0) {
				log.info("Order marked CAPTURED: {}", payload.getRazorpayOrderId());
				// TODO: publish PaymentCapturedEvent to Kafka
				// kafkaTemplate.send("payment-events", payload.getRazorpayOrderId(), event);
			} else {
				log.warn("updateToCaptured affected 0 rows for orderId: {} " +
						"— possible duplicate or wrong state",
						payload.getRazorpayOrderId());
			}
		}
		case "payment.failed" -> {
			int rows = orderRepository.updateToFailed(
					payload.getRazorpayOrderId());
			if (rows > 0) {
				log.info("Order marked FAILED: {}", payload.getRazorpayOrderId());
				// TODO: publish PaymentFailedEvent to Kafka
			}
		}
		default -> log.info("Unhandled webhook event type: {}",
				payload.getEventType());
		}
	}

	private void verifyWebhookSignature(String rawPayload, String signature) {
		boolean valid = signatureVerifier.verify(rawPayload, webhookSecret, signature);
		if (!valid) {
			log.error("Webhook signature mismatch");
			throw new RazorpayProviderException(
					ErrorCode.WEBHOOK_SIGNATURE_VERIFICATION_FAILED,
					Map.of("receivedSignature", signature)
					);
		}
		log.info("Webhook signature verified");
	}

	private WebhookPayload parsePayload(String rawPayload) {
		try {
			JsonNode root = objectMapper.readTree(rawPayload);
			return WebhookPayload.builder()
					.eventType(root.path("event").asText())
					.razorpayEventId(root.path("id").asText())
					.razorpayOrderId(root.path("payload")
							.path("payment").path("entity")
							.path("order_id").asText())
					.razorpayPaymentId(root.path("payload")
							.path("payment").path("entity")
							.path("id").asText())
					.build();
		} catch (Exception e) {
			throw new RazorpayProviderException(
					ErrorCode.INVALID_WEBHOOK_PAYLOAD,
					Map.of("cause", e.getMessage())
					);
		}
	}

	@Data
	@Builder
	private static class WebhookPayload {
		private String eventType;
		private String razorpayEventId;
		private String razorpayOrderId;
		private String razorpayPaymentId;
	}
}
