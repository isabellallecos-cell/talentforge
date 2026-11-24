package com.talentforge.api.access;

/**
 * Actions a member can take inside one organization. Each permission maps to
 * a stable key that the Angular client uses to show or hide screens; the
 * server still checks every request.
 */
public enum Permission {
    MEMBERS_VIEW("members:view"),
    MEMBERS_MANAGE("members:manage"),
    REQUISITIONS_VIEW("requisitions:view"),
    REQUISITIONS_CREATE("requisitions:create"),
    REQUISITIONS_APPROVE("requisitions:approve"),
    CANDIDATES_VIEW("candidates:view"),
    CANDIDATES_MANAGE("candidates:manage"),
    INTERVIEWS_VIEW_ASSIGNED("interviews:view-assigned"),
    SCORECARDS_SUBMIT("scorecards:submit");

    private final String key;

    Permission(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
