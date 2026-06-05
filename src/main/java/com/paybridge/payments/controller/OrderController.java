package com.paybridge.payments.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paybridge.payments.constant.Constant;
import com.paybridge.payments.dto.OrderRequest;
import com.paybridge.payments.dto.OrderResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paybridge.payments.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/internal")
public class OrderController {
	private final OrderService orderService;

	@PostMapping("/orders")
	public OrderResponse createOrder(@RequestBody OrderRequest orderRequest) {
		log.info("createOrder called in OrderController with orderRequest: {}", orderRequest);
		try {
			return orderService.createOrder(orderRequest);
		} catch (Exception e) {
			log.error("Error creating order: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to create order", e);
		}
	}

	@GetMapping("/health")
	public ResponseEntity<OrderResponse> health() throws Exception {
		log.info("Health check for OrderController");
		try {
			OrderResponse response = orderService.createOrder(OrderRequest.builder().amount(100).currency(Constant.DEFAULT_CURRENCY).build());
			log.info("Health check successful, response: {}", response);
		} catch (Exception e) {
			log.error("Health check failed: {}", e.getMessage(), e);
			return ResponseEntity.status(503).build();
		}
		return ResponseEntity.ok(orderService.createOrder(OrderRequest.builder().build()));
	}
}
