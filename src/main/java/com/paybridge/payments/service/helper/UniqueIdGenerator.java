package com.paybridge.payments.service.helper;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class UniqueIdGenerator {

	public String generateUniqueId() {
		return System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
	}

}
