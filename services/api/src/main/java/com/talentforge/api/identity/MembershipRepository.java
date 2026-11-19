package com.talentforge.api.identity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    @Query("""
            select new com.talentforge.api.identity.MembershipView(o.id, o.name, m.role)
            from Membership m join m.organization o
            where m.userId = :userId
            order by o.name
            """)
    List<MembershipView> findViewsByUserId(@Param("userId") UUID userId);
}
