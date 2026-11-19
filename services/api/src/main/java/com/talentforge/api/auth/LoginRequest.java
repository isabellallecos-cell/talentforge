package com.talentforge.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 200) String password) {

    @Override
    public String toString() {
        // Keep the password out of logs and exception messages.
        return "LoginRequest[email=" + email + "]";
    }
}
