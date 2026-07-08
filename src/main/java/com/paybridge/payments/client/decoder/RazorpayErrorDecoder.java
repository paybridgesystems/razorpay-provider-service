package com.paybridge.payments.client.decoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.payments.exception.ErrorCode;
import com.paybridge.payments.exception.RazorpayProviderException;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class RazorpayErrorDecoder implements ErrorDecoder {

	private final ObjectMapper objectMapper;

	@Override
	public Exception decode(String methodKey, Response response) {
		log.error("Razorpay API call failed with status {} for method {}",
				response.status(), methodKey);

		HttpStatus status = HttpStatus.valueOf(response.status());

		String razorpayErrorCode = "UNKNOWN";
		String razorpayDescription = "No details provided";

		if (response.body() != null) {
			try (InputStream body = response.body().asInputStream()) {
				JsonNode root = objectMapper.readTree(body);
				JsonNode error = root.path("error");
				razorpayErrorCode = error.path("code").asText("UNKNOWN");
				razorpayDescription = error.path("description").asText("No details provided");
			} catch (IOException e) {
				log.warn("Could not parse Razorpay error body for status {}", status);
				// defaults stay — that is fine
			}
		} else {
			log.warn("Razorpay returned status {} with no response body", status);
		}

		Map<String, Object> details = Map.of(
				"razorpayErrorCode", razorpayErrorCode,
				"razorpayDescription", razorpayDescription,
				"httpStatus", status.value()
				);
		log.error("Razorpay error details: {}", details);

		// Some Razorpay error codes should be mapped to a domain-specific error
		if ("payment_already_captured".equalsIgnoreCase(razorpayErrorCode)
				|| "payment_capture_failed".equalsIgnoreCase(razorpayErrorCode)
				|| "payment_already_confirmed".equalsIgnoreCase(razorpayErrorCode)) {
			return new RazorpayProviderException(ErrorCode.PAYMENT_CAPTURE_FAILED, details);
		}

		return switch (status) {
		case BAD_REQUEST          -> new RazorpayProviderException(
				ErrorCode.RAZORPAY_BAD_REQUEST, details);
		case UNAUTHORIZED         -> new RazorpayProviderException(
				ErrorCode.RAZORPAY_UNAUTHORIZED, details);
		case UNPROCESSABLE_ENTITY -> new RazorpayProviderException(
				ErrorCode.RAZORPAY_ORDER_CREATION_FAILED, details);
		case TOO_MANY_REQUESTS    -> new RazorpayProviderException(
				ErrorCode.RAZORPAY_RATE_LIMITED, details);
		default                   -> new RazorpayProviderException(
				ErrorCode.RAZORPAY_SERVER_ERROR, details);
		};
	}

}
