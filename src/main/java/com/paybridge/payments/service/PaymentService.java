package com.paybridge.payments.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.paybridge.payments.client.RazorpayClient;
import com.paybridge.payments.client.model.RazorpayCaptureRequest;
import com.paybridge.payments.client.model.RazorpayCaptureResponse;
import com.paybridge.payments.constant.RazorpayConstants;
import com.paybridge.payments.dto.PaymentCaptureRequest;
import com.paybridge.payments.dto.PaymentCaptureResponse;
import com.paybridge.payments.exception.ErrorCode;
import com.paybridge.payments.exception.RazorpayProviderException;
import com.paybridge.payments.repository.RazorpayOrderRepository;
import com.paybridge.payments.repository.entity.RazorpayOrderEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final RazorpayClient razorpayClient;

	private final HmacSignatureVerifier signatureVerifier;

	private final RazorpayOrderRepository orderRepository;

	@Value("${razorpay.key.secret}")
	private String keySecret;

	public PaymentCaptureResponse verifyAndCapture(PaymentCaptureRequest request) {
		log.info("verifyAndCapture called for orderId: {}", request.getRazorpayOrderId());

		verifyPaymentSignature(request);

		log.info("Payment signature verified for orderId: {}", request.getRazorpayOrderId());

		PaymentCaptureResponse captureResponse = capturePayment(request);

		log.info("Payment captured successfully for orderId: {}, paymentId: {}", 
				request.getRazorpayOrderId(), captureResponse.getRazorpayPaymentId());
		return captureResponse;
	}

	private void verifyPaymentSignature(PaymentCaptureRequest request) {
		log.info("Verifying payment signature for orderId: {}", request.getRazorpayOrderId());

		String payload = request.getRazorpayOrderId() + 
				RazorpayConstants.PAYLOAD_SEPARATOR + 
				request.getRazorpayPaymentId();

		boolean valid = signatureVerifier.verify(
				payload,
				keySecret,
				request.getRazorpaySignature()
				);

		if (!valid) {
			log.error("Signature mismatch for orderId: {}", request.getRazorpayOrderId());
			throw new RazorpayProviderException(
					ErrorCode.SIGNATURE_VERIFICATION_FAILED,
					Map.of("razorpayOrderId", request.getRazorpayOrderId())
					);
		}
	}

	private PaymentCaptureResponse capturePayment(PaymentCaptureRequest request) {
		log.info("Capturing payment for paymentId: {}", request.getRazorpayPaymentId());

		RazorpayCaptureRequest captureRequest = RazorpayCaptureRequest.builder()
				.amount(request.getAmount() * RazorpayConstants.RUPEE_CONVERSION_FACTOR)
				.currency(request.getCurrency())
				.build();

		RazorpayCaptureResponse razorpayResponse = razorpayClient.capturePayment(
				request.getRazorpayPaymentId(), captureRequest);

		int rowsUpdated = orderRepository.updateToAuthorized(
				request.getRazorpayOrderId(),
				request.getRazorpayPaymentId(),
				request.getRazorpaySignature()
				);

		if (rowsUpdated == 0) {
			return handleZeroRowsUpdated(request);
		}

		log.info("Payment captured and order updated to AUTHORIZED: {}",
				razorpayResponse.getId());

		return PaymentCaptureResponse.builder()
				.razorpayPaymentId(razorpayResponse.getId())
				.razorpayOrderId(razorpayResponse.getOrderId())
				.status("AUTHORIZED")
				.amount(razorpayResponse.getAmount() / RazorpayConstants.RUPEE_CONVERSION_FACTOR)
				.currency(razorpayResponse.getCurrency())
				.build();
	}

	private PaymentCaptureResponse handleZeroRowsUpdated(PaymentCaptureRequest request) {
		RazorpayOrderEntity existing = orderRepository
				.findByRazorpayOrderId(request.getRazorpayOrderId());

		if (existing != null && isTerminalOrAuthorized(existing.getRazorpayStatus())) {
			log.info("Idempotent capture: order already in state {} for orderId: {}",
					existing.getRazorpayStatus(), request.getRazorpayOrderId());
			return buildResponseFromEntity(existing);
		}

		log.warn("Concurrent transition conflict for orderId: {}",
				request.getRazorpayOrderId());
		throw new RazorpayProviderException(
				ErrorCode.CONCURRENT_TRANSITION_CONFLICT,
				Map.of("razorpayOrderId", request.getRazorpayOrderId())
				);
	}

	private boolean isTerminalOrAuthorized(String status) {
		return "CAPTURED".equalsIgnoreCase(status)
				|| "AUTHORIZED".equalsIgnoreCase(status);
	}

	private PaymentCaptureResponse buildResponseFromEntity(RazorpayOrderEntity existing) {
		return PaymentCaptureResponse.builder()
				.razorpayPaymentId(existing.getRazorpayPaymentId())
				.razorpayOrderId(existing.getRazorpayOrderId())
				.status(existing.getRazorpayStatus() != null
				? existing.getRazorpayStatus()
						: RazorpayConstants.DEFAULT_STATUS)
				.amount((int)(existing.getAmountPaise() / RazorpayConstants.RUPEE_CONVERSION_FACTOR))
				.currency(existing.getCurrency())
				.build();
	}
}
