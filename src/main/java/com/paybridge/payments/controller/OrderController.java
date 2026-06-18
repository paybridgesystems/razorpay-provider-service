package com.paybridge.payments.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paybridge.payments.constant.RazorpayConstants;
import com.paybridge.payments.dto.OrderRequest;
import com.paybridge.payments.dto.OrderResponse;
import com.paybridge.payments.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/internal/orders")
public class OrderController {
	private final OrderService orderService;

	@PostMapping
	public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest orderRequest) {
		log.info("createOrder called in OrderController with orderRequest: {}", orderRequest);
		log.info("Calling OrderService to create order");
		return orderService.createOrder(orderRequest);
	}

	@GetMapping("/health")
	public ResponseEntity<OrderResponse> health() throws Exception {
		log.info("Health check for OrderController");

		ResponseEntity<OrderResponse> response = orderService.createOrder(OrderRequest.builder()
				.amount(100)
				.currency(RazorpayConstants.DEFAULT_CURRENCY)
				.build());
		log.info("Health check successful, response: {}", response);

		return orderService.createOrder(OrderRequest.builder().build());
	}
}
