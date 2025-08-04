package com.musinsa.shop.common.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException() {
        super("Resource Already Exists");
    }

    public DuplicateResourceException(String message) {
        super(message);
    }
}
