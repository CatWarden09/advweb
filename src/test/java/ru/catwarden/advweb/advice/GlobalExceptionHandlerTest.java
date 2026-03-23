package ru.catwarden.advweb.advice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import ru.catwarden.advweb.enums.AdModerationStatus;
import ru.catwarden.advweb.exception.DetailedAccessDeniedException;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.exception.FileOperationException;
import ru.catwarden.advweb.exception.FileStorageException;
import ru.catwarden.advweb.exception.FileTooLargeException;
import ru.catwarden.advweb.exception.InvalidFileTypeException;
import ru.catwarden.advweb.exception.InvalidRelationException;
import ru.catwarden.advweb.exception.InvalidStateException;
import ru.catwarden.advweb.exception.LimitExceededException;
import ru.catwarden.advweb.exception.OperationNotAllowedException;
import ru.catwarden.advweb.validation.dto.ValidationResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleMethodArgumentNotValidReturnsBadRequestWithAllFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "must not be blank"));
        bindingResult.addError(new FieldError("request", "price", "must be greater than 0"));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(Mockito.mock(MethodParameter.class), bindingResult);

        ResponseEntity<ValidationResponse> response = handler.handleMethodArgumentNotValid(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
                List.of("Поле 'name': must not be blank", "Поле 'price': must be greater than 0"),
                response.getBody().getErrors());
    }

    @Test
    void handleConstraintViolationReturnsBadRequestWithAllViolationErrors() {
        ConstraintViolation<?> violation1 = Mockito.mock(ConstraintViolation.class);
        Path path1 = Mockito.mock(Path.class);
        when(violation1.getPropertyPath()).thenReturn(path1);
        when(path1.toString()).thenReturn("createReview.rating");
        when(violation1.getMessage()).thenReturn("must be between 1 and 5");

        ConstraintViolation<?> violation2 = Mockito.mock(ConstraintViolation.class);
        Path path2 = Mockito.mock(Path.class);
        when(violation2.getPropertyPath()).thenReturn(path2);
        when(path2.toString()).thenReturn("createReview.text");
        when(violation2.getMessage()).thenReturn("size must be between 10 and 1000");

        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation1, violation2));

        ResponseEntity<ValidationResponse> response = handler.handleConstraintViolation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getErrors().size());
        assertTrue(response.getBody().getErrors().contains("Поле 'createReview.rating': must be between 1 and 5"));
        assertTrue(response.getBody().getErrors().contains("Поле 'createReview.text': size must be between 10 and 1000"));
    }

    @Test
    void handleEntityNotFoundReturnsNotFound() {
        EntityNotFoundException exception = new EntityNotFoundException(String.class, 10L);

        ResponseEntity<ValidationResponse> response = handler.handleEntityNotFound(exception);

        assertSingleErrorResponse(response, HttpStatus.NOT_FOUND, "String with id=10 not found");
    }

    @Test
    void handleInvalidFileTypeReturnsBadRequest() {
        InvalidFileTypeException exception = new InvalidFileTypeException("Only JPG and PNG files are allowed");

        ResponseEntity<ValidationResponse> response = handler.handleInvalidFileType(exception);

        assertSingleErrorResponse(response, HttpStatus.BAD_REQUEST, "Only JPG and PNG files are allowed");
    }

    @Test
    void handleFileTooLargeReturnsPayloadTooLarge() {
        FileTooLargeException exception = new FileTooLargeException("File size must not exceed 5MB");

        ResponseEntity<ValidationResponse> response = handler.handleFileTooLarge(exception);

        assertSingleErrorResponse(response, HttpStatus.PAYLOAD_TOO_LARGE, "File size must not exceed 5MB");
    }

    @Test
    void handleFileOperationReturnsInternalServerErrorForFileStorageException() {
        FileStorageException exception = new FileStorageException("Could not upload file to storage");

        ResponseEntity<ValidationResponse> response = handler.handleFileOperation(exception);

        assertSingleErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload file to storage");
    }

    @Test
    void handleFileOperationReturnsInternalServerErrorForBaseFileOperationException() {
        FileOperationException exception = new FileOperationException("File operation failed");

        ResponseEntity<ValidationResponse> response = handler.handleFileOperation(exception);

        assertSingleErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "File operation failed");
    }

    @Test
    void handleInvalidRelationReturnsBadRequest() {
        InvalidRelationException exception = new InvalidRelationException("User cannot review himself",
                Map.of("Current user id:", 1, "Recipient user id:", 1));

        ResponseEntity<ValidationResponse> response = handler.handleInvalidRelation(exception);

        assertSingleErrorResponse(response, HttpStatus.BAD_REQUEST, "User cannot review himself");
    }

    @Test
    void handleInvalidStateReturnsConflict() {
        InvalidStateException exception = new InvalidStateException("Review is not in pending status",
                Map.of("Review id:", 1, "Current status:", AdModerationStatus.APPROVED));

        ResponseEntity<ValidationResponse> response = handler.handleInvalidState(exception);

        assertSingleErrorResponse(response, HttpStatus.CONFLICT, "Review is not in pending status");
    }

    @Test
    void handleLimitExceededReturnsBadRequest() {
        LimitExceededException exception = new LimitExceededException("Daily request limit exceeded",
                Map.of("First id", 1, "Second id", 5));

        ResponseEntity<ValidationResponse> response = handler.handleLimitExceeded(exception);

        assertSingleErrorResponse(response, HttpStatus.BAD_REQUEST, "Daily request limit exceeded");
    }

    @Test
    void handleOperationNotAllowedReturnsConflict() {
        OperationNotAllowedException exception = new OperationNotAllowedException("Operation is not allowed in current state",
                Map.of("Some id", 1L));

        ResponseEntity<ValidationResponse> response = handler.handleOperationNotAllowed(exception);

        assertSingleErrorResponse(response, HttpStatus.CONFLICT, "Operation is not allowed in current state");
    }

    @Test
    void handleAccessDeniedReturnsForbidden() {
        DetailedAccessDeniedException exception = new DetailedAccessDeniedException("Access is denied",
                Map.of("Resource type:", "advertisement", "Resource id:", 7L));

        ResponseEntity<ValidationResponse> response = handler.handleAccessDenied(exception);

        assertSingleErrorResponse(response, HttpStatus.FORBIDDEN, "Access is denied");
    }

    private void assertSingleErrorResponse(
            ResponseEntity<ValidationResponse> response,
            HttpStatus expectedStatus,
            String expectedMessage
    ) {
        assertEquals(expectedStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(List.of(expectedMessage), response.getBody().getErrors());
    }
}
