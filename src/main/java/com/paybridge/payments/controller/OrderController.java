package com.paybridge.payments.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paybridge.payments.dto.OrderRequest;
import com.paybridge.payments.dto.OrderResponse;
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
		return orderService.createOrder(orderRequest);
	}

	@GetMapping("/health")
	public ResponseEntity<OrderResponse> health() {
		log.info("Health check for OrderController");
		return ResponseEntity.ok(orderService.createOrder(OrderRequest.builder().build()));
	}
}
