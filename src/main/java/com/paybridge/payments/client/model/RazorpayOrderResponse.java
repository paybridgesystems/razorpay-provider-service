package com.paybridge.payments.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponse {

	private Integer amount;

	@JsonProperty("amount_due")
	private Integer amountDue;

	@JsonProperty("amount_paid")
	private Integer amountPaid;

	private int attempts;

	@JsonProperty("created_at")
	private Long createdAt;

	private String currency;

	private String entity;

	private String id;

	@JsonProperty("offer_id")
	private String offerId;

	private String receipt;

	private String status;
}
