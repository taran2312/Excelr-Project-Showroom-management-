package com.showroom.exception;

/** Business-rule violation (duplicate value, invalid state transition, ...). Mapped to 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
