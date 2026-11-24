package com.talentforge.api.access;

import java.util.UUID;

/**
 * The organization does not exist or the caller is not a member. Both cases
 * answer 404 so organization IDs cannot be probed.
 */
public class OrganizationNotFoundException extends RuntimeException {

    public OrganizationNotFoundException(UUID organizationId) {
        super("Organization " + organizationId + " not found for caller");
    }
}
