package com.talentforge.api.identity;

import java.util.Arrays;

/** Role a user holds inside one organization. */
public enum Role {
    ADMIN("admin"),
    RECRUITER("recruiter"),
    HIRING_MANAGER("hiring_manager"),
    INTERVIEWER("interviewer");

    private final String dbValue;

    Role(String dbValue) {
        this.dbValue = dbValue;
    }

    /** Value stored in {@code memberships.role} and used by the fixtures. */
    public String dbValue() {
        return dbValue;
    }

    public static Role fromDbValue(String value) {
        return Arrays.stream(values())
                .filter(role -> role.dbValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + value));
    }
}
