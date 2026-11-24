package com.talentforge.api.requisition;

import java.util.Arrays;

public enum EmploymentType {
    FULL_TIME("full_time"),
    PART_TIME("part_time"),
    CONTRACT("contract"),
    INTERNSHIP("internship");

    /** Regex used by request validation; must list every {@link #dbValue()}. */
    static final String PATTERN = "full_time|part_time|contract|internship";

    private final String dbValue;

    EmploymentType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static EmploymentType fromDbValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.dbValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown employment type: " + value));
    }
}
