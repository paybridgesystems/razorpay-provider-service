package com.paybridge.payments.client.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderRequest {

    private Integer amount;

    private String currency;

    private String receipt;

    @JsonProperty("payment_capture")
    private Integer paymentCapture;
    
    private Map<String, Object> notes;
}
