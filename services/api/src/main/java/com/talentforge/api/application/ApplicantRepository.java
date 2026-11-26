package com.talentforge.api.application;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicantRepository extends JpaRepository<Applicant, UUID> {

    /**
     * Adds the applicant unless the organization already has one with this
     * email. Concurrent first applications cannot create duplicates or fail on
     * the unique key. Returns 1 if a row was inserted.
     */
    @Modifying
    @Query(value = """
            INSERT INTO applicants (id, organization_id, email, full_name, phone, created_at, updated_at)
            VALUES (:id, :organizationId, :email, :fullName, :phone, :now, :now)
            ON CONFLICT (organization_id, email) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("organizationId") UUID organizationId,
            @Param("email") String email, @Param("fullName") String fullName, @Param("phone") String phone,
            @Param("now") OffsetDateTime now);

    Optional<Applicant> findByOrganizationIdAndEmail(UUID organizationId, String email);
}
