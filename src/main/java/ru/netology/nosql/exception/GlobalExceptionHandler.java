package ru.netology.nosql.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Ошибки валидации тела запроса (@Valid @RequestBody). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException e,
                                                              HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            details.merge(error.getField(), error.getDefaultMessage(), (a, b) -> a + "; " + b);
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
    }

    /** Ошибки валидации параметров запроса и пути (@Min, @Max на аргументах). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParamValidation(HandlerMethodValidationException e,
                                                               HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        e.getParameterValidationResults().forEach(result -> {
            String param = result.getMethodParameter().getParameterName();
            String messages = String.join("; ", result.getResolvableErrors().stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .toList());
            details.put(param, messages);
        });
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                            HttpServletRequest request) {
        String message = "Parameter '" + e.getName() + "' has invalid value '" + e.getValue() + "'";
        return build(HttpStatus.BAD_REQUEST, message, request, Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e,
                                                              HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or malformed JSON", request, Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e,
                                                               HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), request, Map.of());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UserNotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), request, Map.of());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleConflict(EmailAlreadyExistsException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, e.getMessage(), request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request, Map.of());
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                       HttpServletRequest request, Map<String, String> details) {
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), details);
        return ResponseEntity.status(status).body(body);
    }
}
