package com.talentforge.api.requisition;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RequisitionEventResponse(UUID id, String action, String fromStatus, String toStatus, PersonRef actor,
        String note, OffsetDateTime at) {
}
