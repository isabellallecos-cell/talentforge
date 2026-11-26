package com.talentforge.api.posting;

import com.talentforge.api.requisition.EmploymentType;
import com.talentforge.api.requisition.JobRequisition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * An approved requisition as shown on the public careers site. The public
 * fields are copied at publish time, so later requisition edits do not change
 * what applicants already saw. Headcount, hiring manager and approval notes
 * stay internal.
 */
@Entity
@Table(name = "job_postings")
public class JobPosting {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "requisition_id", nullable = false, updatable = false)
    private UUID requisitionId;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(nullable = false, updatable = false)
    private String department;

    @Column(nullable = false, updatable = false)
    private String location;

    @Column(name = "employment_type", nullable = false, updatable = false)
    private EmploymentType employmentType;

    @Column(name = "salary_min", updatable = false)
    private Integer salaryMin;

    @Column(name = "salary_max", updatable = false)
    private Integer salaryMax;

    @Column(name = "salary_currency", updatable = false)
    private String salaryCurrency;

    @Column(updatable = false)
    private String description;

    @Column(nullable = false)
    private PostingStatus status;

    @Column(name = "published_by", nullable = false, updatable = false)
    private UUID publishedBy;

    @Column(name = "published_at", nullable = false, updatable = false)
    private OffsetDateTime publishedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected JobPosting() {
    }

    public JobPosting(UUID id, JobRequisition requisition, UUID publishedBy, OffsetDateTime now) {
        this.id = id;
        this.organizationId = requisition.getOrganizationId();
        this.requisitionId = requisition.getId();
        this.title = requisition.getTitle();
        this.department = requisition.getDepartment();
        this.location = requisition.getLocation();
        this.employmentType = requisition.getEmploymentType();
        this.salaryMin = requisition.getSalaryMin();
        this.salaryMax = requisition.getSalaryMax();
        this.salaryCurrency = requisition.getSalaryCurrency();
        this.description = requisition.getDescription();
        this.status = PostingStatus.OPEN;
        this.publishedBy = publishedBy;
        this.publishedAt = now;
        this.updatedAt = now;
    }

    void close(UUID actorId, OffsetDateTime now) {
        this.status = PostingStatus.CLOSED;
        this.closedBy = actorId;
        this.closedAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getRequisitionId() {
        return requisitionId;
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

    public PostingStatus getStatus() {
        return status;
    }

    public boolean isOpen() {
        return status == PostingStatus.OPEN;
    }

    public UUID getPublishedBy() {
        return publishedBy;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public UUID getClosedBy() {
        return closedBy;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public Long getVersion() {
        return version;
    }
}
