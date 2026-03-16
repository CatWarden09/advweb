package ru.catwarden.advweb.advice;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.validation.dto.ValidationResponse;

import java.util.List;

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
        return new ResponseEntity<>(
                new ValidationResponse(List.of(ex.getMessage())),
                HttpStatus.NOT_FOUND
        );

    }
}