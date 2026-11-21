package com.showroom.exception;

/** Resource is still referenced or in a conflicting state. Mapped to 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
