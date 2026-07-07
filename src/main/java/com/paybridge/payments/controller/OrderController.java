package com.paybridge.payments.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paybridge.payments.dto.OrderRequest;
import com.paybridge.payments.dto.OrderResponse;
import com.paybridge.payments.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/internal/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid OrderRequest orderRequest) {
        log.info("createOrder called with orderRequest: {}", orderRequest);
        
        OrderResponse response = orderService.createOrder(orderRequest);
        
        log.info("Order created successfully with orderId: {}", response.getOrderId());
        return ResponseEntity.ok(response);
    }
}
