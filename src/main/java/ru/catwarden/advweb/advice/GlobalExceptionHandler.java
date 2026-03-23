package ru.catwarden.advweb.advice;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.catwarden.advweb.exception.*;
import ru.catwarden.advweb.validation.dto.ValidationResponse;

import java.util.List;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // used for validating via @Valid on DTO or RequestBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ValidationResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> String.format("Поле '%s': %s", error.getField(), error.getDefaultMessage()))
                .toList();

        return new ResponseEntity<>(new ValidationResponse(errors), HttpStatus.BAD_REQUEST);
    }

    // used for validating via @Validated + annotations on method arguments or entities
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public ResponseEntity<ValidationResponse> handleConstraintViolation(
            ConstraintViolationException ex) {

        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(error -> String.format("Поле '%s': %s",
                        error.getPropertyPath(), error.getMessage()))
                .toList();

        return new ResponseEntity<>(new ValidationResponse(errors), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseBody
    public ResponseEntity<ValidationResponse> handleEntityNotFound(EntityNotFoundException ex) {
        log.error("ENTITY NOT FOUND: {}", ex.getMessage());

        return new ResponseEntity<>(
                new ValidationResponse(List.of(ex.getMessage())),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    @ResponseBody
    public ResponseEntity<ValidationResponse> handleInvalidFileType(InvalidFileTypeException ex) {
        log.error("INVALID FILE TYPE: {}", ex.getMessage());

        return new ResponseEntity<>(
                new ValidationResponse(List.of(ex.getMessage())),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(FileTooLargeException.class)
    @ResponseBody
    public ResponseEntity<ValidationResponse> handleFileTooLarge(FileTooLargeException ex) {
        log.error("FILE TOO LARGE: {}", ex.getMessage());

        return new ResponseEntity<>(
                new ValidationResponse(List.of(ex.getMessage())),
                HttpStatus.PAYLOAD_TOO_LARGE
        );
    }

    @ExceptionHandler({FileStorageException.class, FileOperationException.class})
    @ResponseBody
    public ResponseEntity<ValidationResponse> handleFileOperation(FileOperationException ex) {
        log.error("FILE OPERATION ERROR: {}", ex.getMessage());

        return new ResponseEntity<>(
                new ValidationResponse(List.of(ex.getMessage())),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(InvalidRelationException.class)
    @ResponseBody
    public ResponseEntity<ValidationResponse> handleInvalidRelation(InvalidRelationException ex) {
        log.error("INVALID ENTITY RELATION: {} | details: {}", ex.getMessage(), ex.getDetails());

        return new ResponseEntity<>(
                new ValidationResponse(List.of(ex.getMessage())),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(InvalidStateException.class)
    @ResponseBody
    public ResponseEntity<ValidationResponse> handleInvalidState(InvalidStateException ex) {
        log.error("INVALID ENTITY STATE: {} | details: {}", ex.getMessage(), ex.getDetails());

        return new ResponseEntity<>(
                new ValidationResponse(List.of(ex.getMessage())),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(LimitExceededException.class)
    @ResponseBody
    public ResponseEntity<ValidationResponse> handleLimitExceeded(LimitExceededException ex) {
        log.error("LIMIT EXCEEDED: {} | details: {}", ex.getMessage(), ex.getDetails());

        return new ResponseEntity<>(
                new ValidationResponse(List.of(ex.getMessage())),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(OperationNotAllowedException.class)
    @ResponseBody
    public ResponseEntity<ValidationResponse> handleOperationNotAllowed(OperationNotAllowedException ex) {
        log.error("OPERATION NOT ALLOWED: {}", ex.getMessage());

        return new ResponseEntity<>(
                new ValidationResponse(List.of(ex.getMessage())),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public ResponseEntity<ValidationResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("ACCESS DENIED: {}", ex.getMessage());

        return new ResponseEntity<>(
                new ValidationResponse(List.of(ex.getMessage())),
                HttpStatus.FORBIDDEN
        );
    }
}
