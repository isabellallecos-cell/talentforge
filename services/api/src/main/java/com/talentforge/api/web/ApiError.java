package com.talentforge.api.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Error envelope returned by every API failure. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String code, String message, List<FieldIssue> fields) {

    public ApiError(String code, String message) {
        this(code, message, List.of());
    }

    public record FieldIssue(String field, String message) {
    }
}
