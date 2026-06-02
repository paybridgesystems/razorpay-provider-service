package com.paybridge.payments.service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrderService {

    public String createPayment() {
        log.info("createPayment called");
        return "created";
    }
}
