package com.paybridge.payments.client;

import java.net.ConnectException;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.paybridge.payments.client.config.RazorpayFeignConfig;
import com.paybridge.payments.client.model.RazorpayOrderRequest;
import com.paybridge.payments.client.model.RazorpayOrderResponse;
import com.paybridge.payments.exception.ErrorCode;
import com.paybridge.payments.exception.RazorpayProviderException;

import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(name = "${razorpay.client.name:razorpay-client}", url = "${razorpay.base-url}", configuration = RazorpayFeignConfig.class)
public interface RazorpayClient {
	
	@PostMapping("/v1/orders")
	@Retry(name = "razorpay", fallbackMethod = "createOrderFallback")
	@CircuitBreaker(name = "razorpay")
	RazorpayOrderResponse createOrder(@RequestBody RazorpayOrderRequest request);

	default RazorpayOrderResponse createOrderFallback(RazorpayOrderRequest request, Throwable ex) {

		if (ex instanceof RazorpayProviderException rpe) {
			throw rpe;
		}
		if (ex instanceof CallNotPermittedException) {
			throw new RazorpayProviderException(ErrorCode.CIRCUIT_BREAKER_OPEN);
		}
		if (ex instanceof ConnectException || ex instanceof java.net.UnknownHostException) {
			throw new RazorpayProviderException(ErrorCode.RAZORPAY_UNREACHABLE);
		}
		if (ex instanceof RetryableException) {
			throw new RazorpayProviderException(ErrorCode.RAZORPAY_TIMEOUT);
		}
		
		throw new RazorpayProviderException(
	            ErrorCode.UNEXPECTED_ERROR,
	            Map.of("cause", ex.getMessage() == null ? "unknown" : ex.getMessage())
	        );
	}
}
