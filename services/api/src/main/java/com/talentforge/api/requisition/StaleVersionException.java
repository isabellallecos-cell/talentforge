package com.talentforge.api.requisition;

/** The client edited an older version of the requisition than the one stored. */
public class StaleVersionException extends RuntimeException {

    private final long currentVersion;

    public StaleVersionException(long currentVersion) {
        super("Requisition changed since it was loaded (current version " + currentVersion + ")");
        this.currentVersion = currentVersion;
    }

    public long getCurrentVersion() {
        return currentVersion;
    }
}
