package com.paybridge.payments.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderEntity {
    private Long id;
    private String internalOrderId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private long amountPaise;
    private String currency;
    private String receipt;
    private String razorpayStatus;
    private String checkoutSignature;
}
