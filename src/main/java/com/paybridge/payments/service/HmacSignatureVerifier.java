package com.paybridge.payments.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.paybridge.payments.constant.WebhookConstats;
import com.paybridge.payments.exception.ErrorCode;
import com.paybridge.payments.exception.RazorpayProviderException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HmacSignatureVerifier {

	public String compute(String data, String secret) {
		try {
			Mac mac = Mac.getInstance(WebhookConstats.HMAC_ALGORITHM);
			SecretKeySpec secretKey = new SecretKeySpec(
					secret.getBytes(StandardCharsets.UTF_8),
					WebhookConstats.HMAC_ALGORITHM
					);
			mac.init(secretKey);
			byte[] hash = mac.doFinal(
					data.getBytes(StandardCharsets.UTF_8)
					);
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			log.error(WebhookConstats.HMAC_COMPUTATION_FAILED, e);
			throw new RazorpayProviderException(
					ErrorCode.UNEXPECTED_ERROR,
					Map.of(WebhookConstats.ERROR_CAUSE, WebhookConstats.HMAC_COMPUTATION_FAILED)
					);
		}
	}

	public boolean verify(String data, String secret, String expectedSignature) {
		return compute(data, secret).equals(expectedSignature);
	}
}
