package com.paybridge.payments.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.payments.exception.ErrorCode;
import com.paybridge.payments.exception.RazorpayProviderException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final HmacSignatureVerifier signatureVerifier;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    private final ObjectMapper objectMapper;

    public void handleWebhook(String rawPayload, String razorpaySignature) {
        log.info("Webhook received, verifying signature");

        verifyWebhookSignature(rawPayload, razorpaySignature);

        String event = extractEvent(rawPayload);
        log.info("Webhook event verified and received: {}", event);

        // Kafka publishing added in DB integration branch
        // when full event model and payment state context are available
    }

    private void verifyWebhookSignature(String rawPayload, String signature) {
        // Formula: HMAC-SHA256(rawPayload, webhookSecret)
        // Different secret from PaymentService — intentional
        boolean valid = signatureVerifier.verify(
            rawPayload,
            webhookSecret,
            signature
        );

        if (!valid) {
            log.error("Webhook signature mismatch");
            throw new RazorpayProviderException(
                ErrorCode.WEBHOOK_SIGNATURE_VERIFICATION_FAILED,
                Map.of("receivedSignature", signature)
            );
        }

        log.info("Webhook signature verified successfully");
    }

    private String extractEvent(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            return root.path("event").asText("unknown");
        } catch (Exception e) {
            log.warn("Could not extract event from webhook payload", e);
            throw new RazorpayProviderException(
                ErrorCode.INVALID_WEBHOOK_PAYLOAD,
                Map.of("cause", e.getMessage())
            );
        }
    }
}
