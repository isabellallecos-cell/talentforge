package com.talentforge.api.auth;

import com.talentforge.api.identity.MembershipView;
import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(UUID id, String email, String displayName, List<Membership> memberships) {

    public record Membership(UUID organizationId, String organizationName, String role) {

        static Membership from(MembershipView view) {
            return new Membership(view.organizationId(), view.organizationName(), view.role().dbValue());
        }
    }
}
