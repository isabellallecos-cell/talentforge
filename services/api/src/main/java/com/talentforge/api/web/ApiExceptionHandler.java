package com.talentforge.api.web;

import com.talentforge.api.access.OrganizationNotFoundException;
import com.talentforge.api.careers.PublicJobNotFoundException;
import com.talentforge.api.access.PermissionDeniedException;
import com.talentforge.api.posting.PostingConflictException;
import com.talentforge.api.posting.PostingNotFoundException;
import com.talentforge.api.requisition.ActionNotAllowedException;
import com.talentforge.api.requisition.InvalidTransitionException;
import com.talentforge.api.requisition.RequisitionController.InvalidStatusFilterException;
import com.talentforge.api.requisition.RequisitionController.UnknownActionException;
import com.talentforge.api.requisition.RequisitionNotFoundException;
import com.talentforge.api.requisition.RequisitionValidationException;
import com.talentforge.api.requisition.StaleVersionException;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    @ExceptionHandler(RequisitionValidationException.class)
    ResponseEntity<ApiError> invalidRequisition(RequisitionValidationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError("validation_failed", "Some fields are invalid.", ex.getFields()));
    }

    @ExceptionHandler({RequisitionNotFoundException.class, UnknownActionException.class})
    ResponseEntity<ApiError> requisitionNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("not_found", "Requisition not found."));
    }

    @ExceptionHandler(InvalidStatusFilterException.class)
    ResponseEntity<ApiError> invalidStatusFilter() {
        return ResponseEntity.badRequest().body(new ApiError("invalid_parameter",
                "Parameter 'status' must be one of draft, pending_approval, approved, rejected."));
    }

    @ExceptionHandler(InvalidTransitionException.class)
    ResponseEntity<ApiError> invalidTransition(InvalidTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("invalid_transition", ex.getMessage()));
    }

    @ExceptionHandler({StaleVersionException.class, ObjectOptimisticLockingFailureException.class})
    ResponseEntity<ApiError> staleVersion() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("stale_version",
                "Someone else changed this requisition. Reload it and try again."));
    }

    @ExceptionHandler(ActionNotAllowedException.class)
    ResponseEntity<ApiError> actionNotAllowed(ActionNotAllowedException ex) {
        // The caller can already see the requisition, so the reason reveals nothing new.
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("not_allowed", ex.getMessage()));
    }

    @ExceptionHandler(PostingNotFoundException.class)
    ResponseEntity<ApiError> postingNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("not_found", "Job posting not found."));
    }

    @ExceptionHandler(PostingConflictException.class)
    ResponseEntity<ApiError> postingConflict(PostingConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(PublicJobNotFoundException.class)
    ResponseEntity<ApiError> publicJobNotFound() {
        // Same answer for an unknown organization, an unknown job and a closed job.
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("not_found", "This job is not open for applications."));
    }
}
