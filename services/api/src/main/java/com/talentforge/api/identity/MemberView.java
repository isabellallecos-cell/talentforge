package com.talentforge.api.identity;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Read model of one organization member. */
public record MemberView(UUID membershipId, UUID userId, String displayName, String email, Role role,
        OffsetDateTime joinedAt) {
}
