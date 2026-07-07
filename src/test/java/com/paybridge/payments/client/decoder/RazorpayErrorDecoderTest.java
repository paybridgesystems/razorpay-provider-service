package com.paybridge.payments.client.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.payments.exception.ErrorCode;
import com.paybridge.payments.exception.RazorpayProviderException;

import feign.Response;

class RazorpayErrorDecoderTest {

    private final RazorpayErrorDecoder decoder = new RazorpayErrorDecoder(new ObjectMapper());

    @Test
    void decodeReturnsUnauthorizedExceptionWhenBodyIsMissing() {
        Response response = mock(Response.class);
        when(response.status()).thenReturn(HttpStatus.UNAUTHORIZED.value());
        when(response.body()).thenReturn(null);

        Exception exception = decoder.decode("RazorpayClient#createOrder(RazorpayOrderRequest)", response);

        RazorpayProviderException providerException = assertInstanceOf(RazorpayProviderException.class, exception);
        assertEquals(ErrorCode.RAZORPAY_UNAUTHORIZED, providerException.getErrorCode());
        assertEquals("UNKNOWN", providerException.getDetails().get("razorpayErrorCode"));
        assertEquals("No details provided", providerException.getDetails().get("razorpayDescription"));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), providerException.getDetails().get("httpStatus"));
    }

    @Test
    void decodeReadsRazorpayErrorDetailsWhenJsonBodyIsPresent() throws Exception {
        Response.Body body = mock(Response.Body.class);
        when(body.asInputStream()).thenReturn(new ByteArrayInputStream(
            """
            {"error":{"code":"BAD_REQUEST_ERROR","description":"Invalid request"}}
            """.getBytes(StandardCharsets.UTF_8)
        ));

        Response response = mock(Response.class);
        when(response.status()).thenReturn(HttpStatus.BAD_REQUEST.value());
        when(response.body()).thenReturn(body);

        Exception exception = decoder.decode("RazorpayClient#createOrder(RazorpayOrderRequest)", response);

        RazorpayProviderException providerException = assertInstanceOf(RazorpayProviderException.class, exception);
        assertEquals(ErrorCode.RAZORPAY_BAD_REQUEST, providerException.getErrorCode());
        assertEquals("BAD_REQUEST_ERROR", providerException.getDetails().get("razorpayErrorCode"));
        assertEquals("Invalid request", providerException.getDetails().get("razorpayDescription"));
        assertEquals(HttpStatus.BAD_REQUEST.value(), providerException.getDetails().get("httpStatus"));
        assertTrue(providerException.getDetails().containsKey("razorpayErrorCode"));
    }
}
