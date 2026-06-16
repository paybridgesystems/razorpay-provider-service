package com.paybridge.payments.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.paybridge.payments.client.RazorpayClient;
import com.paybridge.payments.client.model.RazorpayOrderRequest;
import com.paybridge.payments.client.model.RazorpayOrderResponse;
import com.paybridge.payments.constant.Constant;
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
		                        .amount(orderRequest.getAmount() * Constant.RUPEE_CONVERTION)    // RazorPay expects amount in paise, so multiply by 100		
		                        .currency(orderRequest.getCurrency() != null ? orderRequest.getCurrency() : Constant.DEFAULT_CURRENCY)
		                        .receipt(Constant.RECEIPT_ID_TEMPLATE + uniqueIdGenerator.generateUniqueId())
		                        .paymentCapture(Constant.PAYMENT_CAPTURE)    
		                        .build();
		
		try {
			log.info("Calling RazorpayClient with request: {}", razorpayRequest);
	        RazorpayOrderResponse response = razorpayClient.createOrder(razorpayRequest);
	        
	        log.info("Order created successfully with Razorpay, response: {}", response);
	        return ResponseEntity.ok(OrderResponse.builder()
	        		.amount(response.getAmount() / Constant.RUPEE_CONVERTION)
	        		.currency(response.getCurrency())
	        		.orderId(response.getId())
	        		.receipt(response.getReceipt())
	        		.rpStatus(response.getStatus())
	        		.build());
	        
	    } catch (Exception x) {
	    	log.error("Unexpected error while calling Razorpay", x);
	        throw new RuntimeException("Failed to create order with Razorpay", x);
	    }
	}
}
