package com.talentforge.api.posting;

import com.talentforge.api.requisition.PersonRef;
import java.time.OffsetDateTime;
import java.util.UUID;

/** A posting as recruiters see it, with the public careers-site path and its application count. */
public record JobPostingResponse(
        UUID id,
        UUID requisitionId,
        String title,
        String department,
        String location,
        String employmentType,
        String status,
        PersonRef publishedBy,
        OffsetDateTime publishedAt,
        PersonRef closedBy,
        OffsetDateTime closedAt,
        long applicationCount,
        String publicPath,
        long version) {
}
