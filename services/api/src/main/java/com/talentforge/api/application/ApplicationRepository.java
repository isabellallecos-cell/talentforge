package com.talentforge.api.application;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    /**
     * Records an application unless this applicant already applied to the
     * posting. Returns 1 if a row was inserted, 0 for a repeat.
     */
    @Modifying
    @Query(value = """
            INSERT INTO applications (id, organization_id, job_posting_id, applicant_id, status, source,
                                      cover_letter, consented_at, submitted_at, updated_at, version)
            VALUES (:id, :organizationId, :postingId, :applicantId, 'submitted', 'careers_site',
                    :coverLetter, :now, :now, :now, 0)
            ON CONFLICT (job_posting_id, applicant_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("organizationId") UUID organizationId,
            @Param("postingId") UUID postingId, @Param("applicantId") UUID applicantId,
            @Param("coverLetter") String coverLetter, @Param("now") OffsetDateTime now);

    /** Application counts per posting, for the recruiter's posting list. */
    @Query("""
            select a.jobPostingId as postingId, count(a) as total from Application a
            where a.organizationId = :organizationId and a.jobPostingId in :postingIds
            group by a.jobPostingId
            """)
    List<PostingCount> countByPosting(@Param("organizationId") UUID organizationId,
            @Param("postingIds") Collection<UUID> postingIds);

    interface PostingCount {
        UUID getPostingId();

        long getTotal();
    }
}
