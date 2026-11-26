package com.talentforge.api.posting;

import jakarta.validation.constraints.NotNull;

/** Body of {@code POST …/jobs/{id}/close}: the version the caller last saw. */
public record CloseRequest(@NotNull Long version) {
}
