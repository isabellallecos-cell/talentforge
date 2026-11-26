package com.talentforge.api.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One applicant's application to one job posting. Created through
 * {@link ApplicationRepository#insertIfAbsent}, so a repeated submission keeps
 * the first application. Every application starts as {@code submitted}.
 */
@Entity
@Table(name = "applications")
public class Application {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "job_posting_id", nullable = false, updatable = false)
    private UUID jobPostingId;

    @Column(name = "applicant_id", nullable = false, updatable = false)
    private UUID applicantId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, updatable = false)
    private String source;

    @Column(name = "cover_letter", updatable = false)
    private String coverLetter;

    @Column(name = "consented_at", nullable = false, updatable = false)
    private OffsetDateTime consentedAt;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Application() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getJobPostingId() {
        return jobPostingId;
    }

    public UUID getApplicantId() {
        return applicantId;
    }

    public String getStatus() {
        return status;
    }

    public String getSource() {
        return source;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public OffsetDateTime getConsentedAt() {
        return consentedAt;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }
}
