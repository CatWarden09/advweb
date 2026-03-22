package ru.catwarden.advweb.exception;

public class InvalidFileTypeException extends FileOperationException {
    public InvalidFileTypeException(String message) {
        super(message);
    }
}

