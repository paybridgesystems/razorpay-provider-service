package com.paybridge.payments.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.payments.client.decoder.RazorpayErrorDecoder;

import feign.auth.BasicAuthRequestInterceptor;

@Configuration
public class RazorpayFeignConfig {

	@Value("${razorpay.key-id}")
	private String keyId;

	@Value("${razorpay.key-secret}")
	private String keySecret;

	@Bean
	BasicAuthRequestInterceptor razorpayAuthInterceptor() {
		return new BasicAuthRequestInterceptor(keyId, keySecret);
	}

	@Bean
	RazorpayErrorDecoder razorpayErrorDecoder(ObjectMapper objectMapper) {
		return new RazorpayErrorDecoder(objectMapper);
	}

	@Bean
	feign.Logger.Level feignLoggerLevel() {
		return feign.Logger.Level.BASIC;
	}
}
