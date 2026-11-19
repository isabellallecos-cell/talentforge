package com.talentforge.api.web;

import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidFields(MethodArgumentNotValidException ex) {
        List<ApiError.FieldIssue> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldIssue(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ApiError.FieldIssue::field))
                .toList();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError("validation_failed", "Some fields are invalid.", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableBody() {
        return ResponseEntity.badRequest().body(new ApiError("malformed_request", "Request body is not valid JSON."));
    }
}
