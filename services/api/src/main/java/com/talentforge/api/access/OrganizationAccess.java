package com.talentforge.api.access;

import com.talentforge.api.identity.MembershipRepository;
import com.talentforge.api.identity.Role;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the caller's role in an organization from {@code memberships} on
 * every request (roles are not cached in the session, so a role change applies
 * immediately) and enforces the {@link RolePermissions} matrix.
 */
@Service
public class OrganizationAccess {

    private static final Logger log = LoggerFactory.getLogger(OrganizationAccess.class);

    private final MembershipRepository memberships;

    public OrganizationAccess(MembershipRepository memberships) {
        this.memberships = memberships;
    }

    /** Returns the caller's role, or throws 404 if they are not a member. */
    @Transactional(readOnly = true)
    public Role roleIn(UUID userId, UUID organizationId) {
        return memberships.findRole(organizationId, userId)
                .orElseThrow(() -> {
                    log.info("access denied: user {} is not a member of organization {}", userId, organizationId);
                    return new OrganizationNotFoundException(organizationId);
                });
    }

    /** Returns the caller's role if it grants {@code permission}; otherwise 404 or 403. */
    @Transactional(readOnly = true)
    public Role require(UUID userId, UUID organizationId, Permission permission) {
        Role role = roleIn(userId, organizationId);
        if (!RolePermissions.allows(role, permission)) {
            // IDs and role only: never log emails or names.
            log.info("access denied: user {} with role {} lacks {} in organization {}",
                    userId, role.dbValue(), permission.key(), organizationId);
            throw new PermissionDeniedException(permission);
        }
        return role;
    }
}
