package com.talentforge.api.requisition;

/** The caller may see the requisition but not take this action on it. */
public class ActionNotAllowedException extends RuntimeException {

    public ActionNotAllowedException(String reason) {
        super(reason);
    }
}
