package com.bubun.gocardless.exception;

public class GoCardlessTokenException extends RuntimeException {
    public GoCardlessTokenException(String message) {
        super(message);
    }

    public GoCardlessTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
