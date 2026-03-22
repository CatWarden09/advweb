package ru.catwarden.advweb.exception;

public class FileTooLargeException extends FileOperationException {
    public FileTooLargeException(String message) {
        super(message);
    }
}

