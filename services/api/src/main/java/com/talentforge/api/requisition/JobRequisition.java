package com.talentforge.api.requisition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A request to open a position. Status changes go through
 * {@link RequisitionService}, which checks {@link RequisitionWorkflow} first;
 * this class only records the result.
 */
@Entity
@Table(name = "job_requisitions")
public class JobRequisition {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String location;

    @Column(name = "employment_type", nullable = false)
    private EmploymentType employmentType;

    @Column(nullable = false)
    private int headcount;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Column(name = "salary_currency")
    private String salaryCurrency;

    @Column
    private String description;

    @Column(name = "hiring_manager_id", nullable = false)
    private UUID hiringManagerId;

    @Column(nullable = false)
    private RequisitionStatus status;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "submitted_by")
    private UUID submittedBy;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "decision_note")
    private String decisionNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected JobRequisition() {
    }

    public JobRequisition(UUID id, UUID organizationId, RequisitionDetails details, UUID createdBy,
            OffsetDateTime now) {
        this.id = id;
        this.organizationId = organizationId;
        this.status = RequisitionStatus.DRAFT;
        this.createdBy = createdBy;
        this.createdAt = now;
        apply(details, now);
    }

    void apply(RequisitionDetails details, OffsetDateTime now) {
        this.title = details.title();
        this.department = details.department();
        this.location = details.location();
        this.employmentType = details.employmentType();
        this.headcount = details.headcount();
        this.salaryMin = details.salaryMin();
        this.salaryMax = details.salaryMax();
        this.salaryCurrency = details.salaryCurrency();
        this.description = details.description();
        this.hiringManagerId = details.hiringManagerId();
        this.updatedAt = now;
    }

    void moveTo(RequisitionAction action, RequisitionStatus next, UUID actorId, String note, OffsetDateTime now) {
        switch (action) {
            case SUBMIT -> {
                submittedBy = actorId;
                submittedAt = now;
                clearDecision();
            }
            case WITHDRAW, REOPEN -> {
                submittedBy = null;
                submittedAt = null;
                clearDecision();
            }
            case APPROVE, REJECT -> {
                decidedBy = actorId;
                decidedAt = now;
                decisionNote = note;
            }
            default -> throw new IllegalArgumentException("Not a status change: " + action);
        }
        this.status = next;
        this.updatedAt = now;
    }

    private void clearDecision() {
        decidedBy = null;
        decidedAt = null;
        decisionNote = null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getTitle() {
        return title;
    }

    public String getDepartment() {
        return department;
    }

    public String getLocation() {
        return location;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public int getHeadcount() {
        return headcount;
    }

    public Integer getSalaryMin() {
        return salaryMin;
    }

    public Integer getSalaryMax() {
        return salaryMax;
    }

    public String getSalaryCurrency() {
        return salaryCurrency;
    }

    public String getDescription() {
        return description;
    }

    public UUID getHiringManagerId() {
        return hiringManagerId;
    }

    public RequisitionStatus getStatus() {
        return status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getSubmittedBy() {
        return submittedBy;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public OffsetDateTime getDecidedAt() {
        return decidedAt;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** {@code null} until first saved, then 0, 1, … ; clients send it back to detect lost updates. */
    public Long getVersion() {
        return version;
    }
}
