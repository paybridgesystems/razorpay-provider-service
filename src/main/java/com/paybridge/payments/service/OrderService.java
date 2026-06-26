package com.paybridge.payments.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.paybridge.payments.client.RazorpayClient;
import com.paybridge.payments.client.model.RazorpayOrderRequest;
import com.paybridge.payments.client.model.RazorpayOrderResponse;
import com.paybridge.payments.constant.RazorpayConstants;
import com.paybridge.payments.dto.OrderRequest;
import com.paybridge.payments.dto.OrderResponse;
import com.paybridge.payments.service.helper.UniqueIdGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

	private final RazorpayClient razorpayClient;

	private final UniqueIdGenerator uniqueIdGenerator;

	public ResponseEntity<OrderResponse> createOrder(OrderRequest orderRequest) throws RuntimeException {
		log.info("createOrder called in OrderService with orderRequest: {}", orderRequest);

		RazorpayOrderRequest razorpayRequest = 
				RazorpayOrderRequest.builder()
				.amount(orderRequest.getAmount() * RazorpayConstants.RUPEE_CONVERSION_FACTOR)		
				.currency(orderRequest.getCurrency())
				.receipt(RazorpayConstants.RECEIPT_ID_PREFIX + uniqueIdGenerator.generateUniqueId())
				.paymentCapture(RazorpayConstants.PAYMENT_CAPTURE_DISABLED)    
				.build();

		log.info("Calling RazorpayClient with request: {}", razorpayRequest);
		RazorpayOrderResponse response = razorpayClient.createOrder(razorpayRequest);

		log.info("Order created successfully with Razorpay, response: {}", response);
		return ResponseEntity.ok(OrderResponse.builder()
				.amount(response.getAmount() / RazorpayConstants.RUPEE_CONVERSION_FACTOR)
				.currency(response.getCurrency())
				.orderId(response.getId())
				.receipt(response.getReceipt())
				.rpStatus(response.getStatus())
				.build());

	}
}
