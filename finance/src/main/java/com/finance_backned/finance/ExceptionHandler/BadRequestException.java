package com.finance_backned.finance.ExceptionHandler;



public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
