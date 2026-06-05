package com.paybridge.payments.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

	private Integer amount;

	private String currency;

	private String orderId;

	private String receipt;

	private String rpStatus;
}