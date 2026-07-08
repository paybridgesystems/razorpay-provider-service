package com.paybridge.payments.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayPaymentEventEntity {
    private Long id;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String eventType;
    private String rawPayload;
    private String razorpayEventId;
    private boolean processed;
}
