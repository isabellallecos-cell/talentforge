package com.talentforge.api.requisition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** One entry of a requisition's append-only history (a database trigger rejects UPDATE and direct DELETE). */
@Entity
@Table(name = "requisition_events")
public class RequisitionEvent {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "requisition_id", nullable = false, updatable = false)
    private UUID requisitionId;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;

    @Column(nullable = false, updatable = false)
    private RequisitionAction action;

    @Column(name = "from_status", updatable = false)
    private RequisitionStatus fromStatus;

    @Column(name = "to_status", nullable = false, updatable = false)
    private RequisitionStatus toStatus;

    @Column(updatable = false)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected RequisitionEvent() {
    }

    public RequisitionEvent(JobRequisition requisition, UUID actorId, RequisitionAction action,
            RequisitionStatus fromStatus, String note, OffsetDateTime now) {
        this.id = UUID.randomUUID();
        this.organizationId = requisition.getOrganizationId();
        this.requisitionId = requisition.getId();
        this.actorId = actorId;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = requisition.getStatus();
        this.note = note;
        this.createdAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequisitionId() {
        return requisitionId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public RequisitionAction getAction() {
        return action;
    }

    public RequisitionStatus getFromStatus() {
        return fromStatus;
    }

    public RequisitionStatus getToStatus() {
        return toStatus;
    }

    public String getNote() {
        return note;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
