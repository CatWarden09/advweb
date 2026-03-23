package ru.catwarden.advweb.exception;

import java.util.Map;

public class InvalidRelationException extends RuntimeException {
    private final Map<String, Object> details;

    public InvalidRelationException(String message, Map<String, Object> details) {
        super(message);
        this.details = details;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
