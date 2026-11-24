package com.talentforge.api.requisition;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequisitionEventRepository extends JpaRepository<RequisitionEvent, UUID> {

    List<RequisitionEvent> findByOrganizationIdAndRequisitionIdOrderByCreatedAtAscIdAsc(UUID organizationId,
            UUID requisitionId);
}
