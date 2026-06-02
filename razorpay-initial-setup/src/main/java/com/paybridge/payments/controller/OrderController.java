package com.paybridge.payments.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public void createOrder() {
		log.info("createOrder called");
		orderService.createPayment();
	}

    @GetMapping
    public ResponseEntity<String> health() {
        log.info("Health check for ProviderController");
        return ResponseEntity.ok(orderService.createPayment());
    }
}
