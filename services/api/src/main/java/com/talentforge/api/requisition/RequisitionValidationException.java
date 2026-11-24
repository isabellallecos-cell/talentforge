package com.talentforge.api.requisition;

import com.talentforge.api.web.ApiError;
import java.util.List;

/** Field problems that need the database (hiring manager) or more than one field (salary range). */
public class RequisitionValidationException extends RuntimeException {

    private final List<ApiError.FieldIssue> fields;

    public RequisitionValidationException(List<ApiError.FieldIssue> fields) {
        super("Invalid requisition");
        this.fields = List.copyOf(fields);
    }

    public List<ApiError.FieldIssue> getFields() {
        return fields;
    }
}
