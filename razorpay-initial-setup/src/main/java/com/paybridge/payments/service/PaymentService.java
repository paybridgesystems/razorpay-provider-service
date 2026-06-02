package com.paybridge.payments.service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PaymentService {

    public String capturePayment() {
        log.info("capturePayment called");
        return "captured";
    }
}
