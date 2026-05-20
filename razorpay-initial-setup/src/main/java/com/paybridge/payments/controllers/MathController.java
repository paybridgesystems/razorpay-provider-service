package com.paybridge.payments.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class MathController {
	
	@GetMapping("payments/add")
	public int add(@RequestParam int a, @RequestParam int b) {
		log.info("Received request to add {} and {}", a, b);
		return a + b;
	}
	
}
