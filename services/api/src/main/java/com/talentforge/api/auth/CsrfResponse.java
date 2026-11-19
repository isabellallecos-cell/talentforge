package com.talentforge.api.auth;

public record CsrfResponse(String headerName, String token) {
}
