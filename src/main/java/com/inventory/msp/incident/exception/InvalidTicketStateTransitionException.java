package com.inventory.msp.incident.exception;

public class InvalidTicketStateTransitionException extends RuntimeException {

    public InvalidTicketStateTransitionException(String message) {
        super(message);
    }

    public InvalidTicketStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}

