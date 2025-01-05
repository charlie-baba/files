package com.strac.files.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;

/**
 * @author Charles on 22/12/2024
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // Handle RecordNotFoundException
    @ExceptionHandler(RecordNotFoundException.class)
    public ResponseEntity<Object> handleRecordNotFoundException(
            RecordNotFoundException ex, WebRequest request) {

        Map<String, Object> body = getStringObjectMap(ex.getMessage(), request);
        log.error(ex.getMessage(), ex);

        return new ResponseEntity<>(body, NOT_FOUND);
    }

    // Handle UnauthorizedException
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {

        Map<String, Object> body = getStringObjectMap(ex.getMessage(), request);
        log.error(ex.getMessage(), ex);

        return new ResponseEntity<>(body, UNAUTHORIZED);
    }

    // Handle MaxUploadSizeExceededException
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxSizeException(MaxUploadSizeExceededException ex, WebRequest request) {
        Map<String, Object> body = getStringObjectMap("File exceeds maximum upload size", request);
        log.error("Error: {}", ex.getMessage(), ex);

        return new ResponseEntity<>(body, PAYLOAD_TOO_LARGE);
    }

    // Handle generic exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(
            Exception ex, WebRequest request) {

        Map<String, Object> body = getStringObjectMap("An error occurred", request);
        log.error(ex.getMessage(), ex);

        return new ResponseEntity<>(body, INTERNAL_SERVER_ERROR);
    }

    private static Map<String, Object> getStringObjectMap(String message, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", message);
        body.put("details", request.getDescription(false));
        return body;
    }
}
