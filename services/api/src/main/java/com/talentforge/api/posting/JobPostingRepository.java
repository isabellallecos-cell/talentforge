package com.talentforge.api.posting;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

    /** Always look postings up together with the organization from the URL. */
    Optional<JobPosting> findByOrganizationIdAndId(UUID organizationId, UUID id);

    Optional<JobPosting> findByOrganizationIdAndRequisitionId(UUID organizationId, UUID requisitionId);

    List<JobPosting> findByOrganizationIdOrderByPublishedAtDescIdAsc(UUID organizationId);

    List<JobPosting> findByOrganizationIdAndStatusOrderByPublishedAtDescIdAsc(UUID organizationId,
            PostingStatus status);

    /**
     * Reads a posting with a shared row lock, so an application and a
     * concurrent close cannot interleave: closing waits for the application to
     * commit, and an application started after the close sees it closed.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select p from JobPosting p where p.organizationId = :organizationId and p.id = :id")
    Optional<JobPosting> findForApplication(@Param("organizationId") UUID organizationId, @Param("id") UUID id);
}
