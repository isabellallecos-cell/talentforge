package com.talentforge.api.requisition;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** A requisition plus the actions the caller can take on it right now. */
public record RequisitionResponse(
        UUID id,
        UUID organizationId,
        String title,
        String department,
        String location,
        String employmentType,
        int headcount,
        Integer salaryMin,
        Integer salaryMax,
        String salaryCurrency,
        String description,
        PersonRef hiringManager,
        String status,
        PersonRef createdBy,
        PersonRef submittedBy,
        OffsetDateTime submittedAt,
        PersonRef decidedBy,
        OffsetDateTime decidedAt,
        String decisionNote,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long version,
        List<String> allowedActions) {
}
