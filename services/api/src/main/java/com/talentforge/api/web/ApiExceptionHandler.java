package com.talentforge.api.web;

import com.talentforge.api.access.OrganizationNotFoundException;
import com.talentforge.api.access.PermissionDeniedException;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> invalidParameter(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(new ApiError("invalid_parameter",
                "Parameter '" + ex.getName() + "' has an invalid format."));
    }

    @ExceptionHandler(OrganizationNotFoundException.class)
    ResponseEntity<ApiError> organizationNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("not_found", "Organization not found."));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    ResponseEntity<ApiError> permissionDenied() {
        // Same body as Spring Security's 403 so clients handle one shape.
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("forbidden", "You do not have permission for this action."));
    }
}
