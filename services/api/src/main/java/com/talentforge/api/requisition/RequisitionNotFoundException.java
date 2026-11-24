package com.talentforge.api.requisition;

import java.util.UUID;

/** No requisition with this ID in the organization from the URL. */
public class RequisitionNotFoundException extends RuntimeException {

    public RequisitionNotFoundException(UUID requisitionId) {
        super("Requisition " + requisitionId + " not found");
    }
}
