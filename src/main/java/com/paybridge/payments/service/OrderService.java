package com.paybridge.payments.service;

import org.springframework.stereotype.Service;

import com.paybridge.payments.client.RazorpayClient;
import com.paybridge.payments.client.model.RazorpayOrderRequest;
import com.paybridge.payments.client.model.RazorpayOrderResponse;
import com.paybridge.payments.constant.RazorpayConstants;
import com.paybridge.payments.dto.OrderRequest;
import com.paybridge.payments.dto.OrderResponse;
import com.paybridge.payments.repository.RazorpayOrderRepository;
import com.paybridge.payments.repository.entity.RazorpayOrderEntity;
import com.paybridge.payments.service.helper.UniqueIdGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final RazorpayClient razorpayClient;
    private final UniqueIdGenerator uniqueIdGenerator;
    private final RazorpayOrderRepository orderRepository;

    public OrderResponse createOrder(OrderRequest orderRequest) {
        log.info("createOrder called with orderRequest: {}", orderRequest);

        String internalOrderId = uniqueIdGenerator.generateUniqueId();

        RazorpayOrderRequest razorpayRequest = RazorpayOrderRequest.builder()
            .amount(orderRequest.getAmount() * RazorpayConstants.RUPEE_CONVERSION_FACTOR)
            .currency(orderRequest.getCurrency())
            .receipt(RazorpayConstants.RECEIPT_ID_PREFIX + internalOrderId)
            .paymentCapture(RazorpayConstants.PAYMENT_CAPTURE_DISABLED)
            .build();

        log.info("Calling RazorpayClient with request: {}", razorpayRequest);
        RazorpayOrderResponse razorpayResponse = razorpayClient.createOrder(razorpayRequest);

        log.info("Order created successfully, razorpayOrderId: {}", razorpayResponse.getId());
        orderRepository.insert(RazorpayOrderEntity.builder()
            .internalOrderId(internalOrderId)
            .razorpayOrderId(razorpayResponse.getId())
            .amountPaise(razorpayResponse.getAmount())
            .currency(razorpayResponse.getCurrency())
            .receipt(razorpayResponse.getReceipt())
            .build());

        log.info("Order persisted, razorpayOrderId: {}", razorpayResponse.getId());
        return OrderResponse.builder()
            .amount(razorpayResponse.getAmount() / RazorpayConstants.RUPEE_CONVERSION_FACTOR)
            .currency(razorpayResponse.getCurrency())
            .orderId(razorpayResponse.getId())
            .receipt(razorpayResponse.getReceipt())
            .rpStatus(razorpayResponse.getStatus())
            .build();
    }
}