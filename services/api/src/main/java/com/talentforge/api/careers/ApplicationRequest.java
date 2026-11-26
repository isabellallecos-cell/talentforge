package com.talentforge.api.careers;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /v1/public/organizations/{slug}/jobs/{id}/applications}.
 * Sent by anonymous visitors, so every field is bounded and single-line fields
 * refuse control characters.
 */
public record ApplicationRequest(
        @NotBlank @Size(max = 120) @Pattern(regexp = ApplicationRequest.SINGLE_LINE,
                message = "must not contain control characters") String fullName,
        @NotBlank @Size(max = 254) @Pattern(regexp = "\\s*[^@\\s]+@[^@\\s]+\\.[^@\\s]+\\s*",
                message = "must be a valid email address") String email,
        @Size(max = 40) @Pattern(regexp = "[0-9+()\\-. ]*",
                message = "may only contain digits, spaces and + ( ) - .") String phone,
        @Size(max = 4000) String coverLetter,
        @NotNull @AssertTrue(message = "must be accepted to apply") Boolean privacyConsent) {

    static final String SINGLE_LINE = "[^\\p{Cntrl}]*";
}
