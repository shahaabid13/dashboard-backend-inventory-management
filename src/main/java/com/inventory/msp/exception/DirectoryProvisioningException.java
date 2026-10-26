package com.inventory.msp.exception;

import org.springframework.http.HttpStatus;

public class DirectoryProvisioningException extends RuntimeException {

    private final HttpStatus status;

    public DirectoryProvisioningException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
