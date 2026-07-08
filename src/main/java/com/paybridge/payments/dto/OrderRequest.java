package com.paybridge.payments.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

	@NotNull(message = "Amount is required")
	@Positive(message = "Amount must be greater than zero")
	private Integer amount;

	@NotBlank(message = "Currency is required")
	private String currency;

	private Map<String, Object> notes;
}
