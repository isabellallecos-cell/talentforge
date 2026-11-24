package com.talentforge.api.requisition;

import static com.talentforge.api.requisition.RequisitionAction.APPROVE;
import static com.talentforge.api.requisition.RequisitionAction.REJECT;
import static com.talentforge.api.requisition.RequisitionAction.REOPEN;
import static com.talentforge.api.requisition.RequisitionAction.SUBMIT;
import static com.talentforge.api.requisition.RequisitionAction.UPDATE;
import static com.talentforge.api.requisition.RequisitionAction.WITHDRAW;

import com.talentforge.api.access.Permission;
import com.talentforge.api.access.RolePermissions;
import com.talentforge.api.identity.Role;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Requisition lifecycle and who may move it. No I/O, so every rule is unit
 * tested in {@code RequisitionWorkflowTest}.
 *
 * <pre>
 * draft ──submit──▶ pending_approval ──approve──▶ approved
 *   ▲  ▲               │      │
 *   │  └───withdraw────┘      └──reject──▶ rejected
 *   └────────────────reopen──────────────────┘
 * </pre>
 */
public final class RequisitionWorkflow {

    private static final Map<RequisitionStatus, Map<RequisitionAction, RequisitionStatus>> TRANSITIONS =
            new EnumMap<>(RequisitionStatus.class);

    static {
        TRANSITIONS.put(RequisitionStatus.DRAFT, Map.of(
                UPDATE, RequisitionStatus.DRAFT,
                SUBMIT, RequisitionStatus.PENDING_APPROVAL));
        TRANSITIONS.put(RequisitionStatus.PENDING_APPROVAL, Map.of(
                WITHDRAW, RequisitionStatus.DRAFT,
                APPROVE, RequisitionStatus.APPROVED,
                REJECT, RequisitionStatus.REJECTED));
        TRANSITIONS.put(RequisitionStatus.REJECTED, Map.of(REOPEN, RequisitionStatus.DRAFT));
        // Approved is final for now; publishing and closing arrive with the applicant portal.
        TRANSITIONS.put(RequisitionStatus.APPROVED, Map.of());
    }

    /** Status changes a client may request through {@code POST …/requisitions/{id}/{action}}. */
    public static final Set<RequisitionAction> STATUS_ACTIONS = EnumSet.of(SUBMIT, WITHDRAW, APPROVE, REJECT, REOPEN);

    private RequisitionWorkflow() {
    }

    /** The status after {@code action}, or empty if the action is not allowed from {@code from}. */
    public static Optional<RequisitionStatus> next(RequisitionStatus from, RequisitionAction action) {
        return Optional.ofNullable(TRANSITIONS.get(from).get(action));
    }

    /** The role permission an action needs before any per-requisition rule is checked. */
    public static Permission permissionFor(RequisitionAction action) {
        return switch (action) {
            case APPROVE, REJECT -> Permission.REQUISITIONS_APPROVE;
            case CREATE, UPDATE, SUBMIT, WITHDRAW, REOPEN -> Permission.REQUISITIONS_CREATE;
        };
    }

    /**
     * Why {@code actorId} (holding {@code role}) may not take {@code action} on
     * {@code requisition}, or empty if they may. Covers the role permission and
     * the per-requisition rules, not the status.
     */
    public static Optional<String> denialReason(JobRequisition requisition, RequisitionAction action, UUID actorId,
            Role role) {
        if (!RolePermissions.allows(role, permissionFor(action))) {
            return Optional.of("Your role cannot " + action.dbValue() + " requisitions.");
        }
        return switch (action) {
            case WITHDRAW -> actorId.equals(requisition.getSubmittedBy()) || role == Role.ADMIN
                    ? Optional.empty()
                    : Optional.of("Only the person who submitted this requisition or an admin can withdraw it.");
            case APPROVE, REJECT -> {
                // Four-eyes: applies to admins too.
                if (actorId.equals(requisition.getSubmittedBy())) {
                    yield Optional.of("You cannot " + action.dbValue() + " a requisition you submitted.");
                }
                if (role != Role.ADMIN && !actorId.equals(requisition.getHiringManagerId())) {
                    yield Optional.of("Only the assigned hiring manager or an admin can "
                            + action.dbValue() + " this requisition.");
                }
                yield Optional.empty();
            }
            default -> Optional.empty();
        };
    }

    /** Actions the caller can take right now; the web app shows exactly these buttons. */
    public static Set<RequisitionAction> allowedActions(JobRequisition requisition, UUID actorId, Role role) {
        Set<RequisitionAction> allowed = EnumSet.noneOf(RequisitionAction.class);
        for (RequisitionAction action : TRANSITIONS.get(requisition.getStatus()).keySet()) {
            if (denialReason(requisition, action, actorId, role).isEmpty()) {
                allowed.add(action);
            }
        }
        return allowed;
    }
}
