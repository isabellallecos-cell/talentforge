package com.talentforge.api.posting;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Body of {@code POST /v1/organizations/{id}/jobs}: the approved requisition to publish. */
public record PublishRequest(@NotNull UUID requisitionId) {
}
