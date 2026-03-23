package ru.catwarden.advweb.exception;

import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

public class DetailedAccessDeniedException extends AccessDeniedException {
    private final Map<String, Object> details;

    public DetailedAccessDeniedException(String message, Map<String, Object> details) {
        super(message);
        this.details = details;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
