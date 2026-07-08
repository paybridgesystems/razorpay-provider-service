package com.paybridge.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class RazorpayProviderServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(RazorpayProviderServiceApplication.class, args);
	}
}
