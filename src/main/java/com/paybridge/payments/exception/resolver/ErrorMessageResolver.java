package com.paybridge.payments.exception.resolver;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import com.paybridge.payments.exception.ErrorCode;

@Component
public class ErrorMessageResolver {

    private final MessageSource messageSource;

    public ErrorMessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String resolve(ErrorCode errorCode) {
        return messageSource.getMessage(String.valueOf(errorCode.getCode()), null, null);
    }
}