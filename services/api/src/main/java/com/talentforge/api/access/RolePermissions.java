package com.talentforge.api.access;

import static com.talentforge.api.access.Permission.CANDIDATES_MANAGE;
import static com.talentforge.api.access.Permission.CANDIDATES_VIEW;
import static com.talentforge.api.access.Permission.INTERVIEWS_VIEW_ASSIGNED;
import static com.talentforge.api.access.Permission.JOBS_PUBLISH;
import static com.talentforge.api.access.Permission.MEMBERS_MANAGE;
import static com.talentforge.api.access.Permission.MEMBERS_VIEW;
import static com.talentforge.api.access.Permission.REQUISITIONS_APPROVE;
import static com.talentforge.api.access.Permission.REQUISITIONS_CREATE;
import static com.talentforge.api.access.Permission.REQUISITIONS_VIEW;
import static com.talentforge.api.access.Permission.SCORECARDS_SUBMIT;

import com.talentforge.api.identity.Role;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The role/permission matrix. Kept in code (not the database) so a change is
 * reviewed like any other change and covered by {@code RolePermissionsTest};
 * docs/security/role-permissions.md describes the same table.
 */
public final class RolePermissions {

    private static final Map<Role, Set<Permission>> MATRIX = new EnumMap<>(Role.class);

    static {
        MATRIX.put(Role.ADMIN, EnumSet.allOf(Permission.class));
        MATRIX.put(Role.RECRUITER, EnumSet.of(
                MEMBERS_VIEW, REQUISITIONS_VIEW, REQUISITIONS_CREATE, JOBS_PUBLISH,
                CANDIDATES_VIEW, CANDIDATES_MANAGE, INTERVIEWS_VIEW_ASSIGNED));
        MATRIX.put(Role.HIRING_MANAGER, EnumSet.of(
                REQUISITIONS_VIEW, REQUISITIONS_CREATE, REQUISITIONS_APPROVE,
                CANDIDATES_VIEW, INTERVIEWS_VIEW_ASSIGNED, SCORECARDS_SUBMIT));
        // Interviewers only see what they are assigned to and submit their own
        // scorecards; they never browse requisitions or the candidate pool.
        MATRIX.put(Role.INTERVIEWER, EnumSet.of(INTERVIEWS_VIEW_ASSIGNED, SCORECARDS_SUBMIT));
        MATRIX.replaceAll((role, permissions) -> Collections.unmodifiableSet(permissions));
    }

    private RolePermissions() {
    }

    public static Set<Permission> of(Role role) {
        return MATRIX.get(role);
    }

    public static boolean allows(Role role, Permission permission) {
        return MATRIX.get(role).contains(permission);
    }
}
