package com.paybridge.payments.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.paybridge.payments.client.config.RazorpayFeignConfig;
import com.paybridge.payments.client.model.RazorpayOrderRequest;
import com.paybridge.payments.client.model.RazorpayOrderResponse;

@FeignClient(
	name = "razorpay-client", 
	url = "${razorpay.base-url}", 
	configuration = RazorpayFeignConfig.class
)
public interface RazorpayClient {

	@PostMapping("/v1/orders")
	RazorpayOrderResponse createOrder(@RequestBody RazorpayOrderRequest request);
}
