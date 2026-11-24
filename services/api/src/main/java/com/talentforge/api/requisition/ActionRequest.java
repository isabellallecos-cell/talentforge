package com.talentforge.api.requisition;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST …/requisitions/{id}/{action}}. {@code version} is the
 * version the caller last saw; {@code note} is required to reject.
 */
public record ActionRequest(@NotNull Long version, @Size(max = 2000) String note) {
}
