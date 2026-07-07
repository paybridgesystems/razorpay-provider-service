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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final RazorpayClient razorpayClient;
    
    private final HmacSignatureVerifier signatureVerifier;

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
            request.getRazorpayPaymentId(),
            captureRequest
        );

        log.info("Payment captured successfully, paymentId: {}", razorpayResponse.getId());

        return PaymentCaptureResponse.builder()
            .razorpayPaymentId(razorpayResponse.getId())
            .razorpayOrderId(razorpayResponse.getOrderId())
            .status(RazorpayConstants.STATUS_AUTHORIZED)
            .amount(razorpayResponse.getAmount() / RazorpayConstants.RUPEE_CONVERSION_FACTOR)
            .currency(razorpayResponse.getCurrency())
            .build();
    }
}
