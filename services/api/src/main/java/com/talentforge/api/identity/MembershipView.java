package com.talentforge.api.identity;

import java.util.UUID;

/** Read model of one membership with its organization name. */
public record MembershipView(UUID organizationId, String organizationName, Role role) {
}
