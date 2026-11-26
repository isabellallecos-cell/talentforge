package com.talentforge.api.posting;

/** The posting request conflicts with the current state (409 with {@code code}). */
public class PostingConflictException extends RuntimeException {

    private final String code;

    public PostingConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
