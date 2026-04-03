package com.finance_backned.finance.ExceptionHandler;



public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
