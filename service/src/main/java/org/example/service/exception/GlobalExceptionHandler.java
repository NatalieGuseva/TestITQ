package org.example.service.exception;


import lombok.extern.slf4j.Slf4j;
import org.example.service.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 404 — документ не найден
     */
    @ExceptionHandler(DocumentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleDocumentNotFound(DocumentNotFoundException ex) {
        log.warn("Документ не найден: {}", ex.getMessage());
        return ErrorResponse.of("DOCUMENT_NOT_FOUND", ex.getMessage());
    }

    /**
     * 409 — недопустимый переход статуса
     */
    @ExceptionHandler(InvalidStatusTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInvalidTransition(InvalidStatusTransitionException ex) {
        log.warn("Недопустимый переход статуса: {}", ex.getMessage());
        return ErrorResponse.of("INVALID_STATUS_TRANSITION", ex.getMessage());
    }

    /**
     * 400 — ошибки валидации входных данных (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Ошибка валидации: {}", message);
        return ErrorResponse.of("VALIDATION_ERROR", message);
    }

    /**
     * 400 — некорректные параметры запроса
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Ошибка валидации параметров: {}", message);
        return ErrorResponse.of("VALIDATION_ERROR", message);
    }

    /**
     * 500 — все остальные неожиданные ошибки
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericError(Exception ex) {
        log.error("Неожиданная ошибка: {}", ex.getMessage(), ex);
        return ErrorResponse.of("INTERNAL_ERROR", "Внутренняя ошибка сервера");
    }
}
