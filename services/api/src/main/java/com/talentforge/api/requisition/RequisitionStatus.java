package com.talentforge.api.requisition;

import java.util.Arrays;

public enum RequisitionStatus {
    DRAFT("draft"),
    PENDING_APPROVAL("pending_approval"),
    APPROVED("approved"),
    REJECTED("rejected");

    private final String dbValue;

    RequisitionStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    /** Value stored in the database and returned by the API. */
    public String dbValue() {
        return dbValue;
    }

    public static RequisitionStatus fromDbValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.dbValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown requisition status: " + value));
    }
}
