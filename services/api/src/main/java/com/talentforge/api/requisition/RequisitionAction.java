package com.talentforge.api.requisition;

import java.util.Arrays;
import java.util.Optional;

/** Something that happens to a requisition; each one is recorded in {@code requisition_events}. */
public enum RequisitionAction {
    CREATE("create"),
    UPDATE("update"),
    SUBMIT("submit"),
    WITHDRAW("withdraw"),
    APPROVE("approve"),
    REJECT("reject"),
    REOPEN("reopen");

    private final String dbValue;

    RequisitionAction(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static RequisitionAction fromDbValue(String value) {
        return fromPath(value).orElseThrow(() -> new IllegalArgumentException("Unknown requisition action: " + value));
    }

    /** Parses the action segment of {@code POST …/requisitions/{id}/{action}}. */
    public static Optional<RequisitionAction> fromPath(String value) {
        return Arrays.stream(values()).filter(action -> action.dbValue.equals(value)).findFirst();
    }
}
