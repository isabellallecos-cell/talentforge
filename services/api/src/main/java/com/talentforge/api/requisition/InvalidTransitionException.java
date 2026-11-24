package com.talentforge.api.requisition;

/** The action is not possible from the requisition's current status. */
public class InvalidTransitionException extends RuntimeException {

    private final RequisitionStatus status;
    private final RequisitionAction action;

    public InvalidTransitionException(RequisitionStatus status, RequisitionAction action) {
        super("Cannot " + action.dbValue() + " a requisition that is " + status.dbValue().replace('_', ' ') + ".");
        this.status = status;
        this.action = action;
    }

    public RequisitionStatus getStatus() {
        return status;
    }

    public RequisitionAction getAction() {
        return action;
    }
}
