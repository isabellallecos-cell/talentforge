package com.talentforge.api.requisition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRequisitionRepository extends JpaRepository<JobRequisition, UUID> {

    /** Always look requisitions up together with the organization from the URL. */
    Optional<JobRequisition> findByOrganizationIdAndId(UUID organizationId, UUID id);

    List<JobRequisition> findByOrganizationIdOrderByUpdatedAtDescIdAsc(UUID organizationId);

    List<JobRequisition> findByOrganizationIdAndStatusOrderByUpdatedAtDescIdAsc(UUID organizationId,
            RequisitionStatus status);
}
