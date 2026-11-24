package com.talentforge.api.organization;

import com.talentforge.api.identity.MemberView;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MemberResponse(UUID membershipId, UUID userId, String displayName, String email, String role,
        OffsetDateTime joinedAt) {

    static MemberResponse from(MemberView view) {
        return new MemberResponse(view.membershipId(), view.userId(), view.displayName(), view.email(),
                view.role().dbValue(), view.joinedAt());
    }
}
