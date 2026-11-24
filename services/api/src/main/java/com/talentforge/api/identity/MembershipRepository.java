package com.talentforge.api.identity;

import java.util.List;
import java.util.Optional;
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

    @Query("""
            select m.role from Membership m
            where m.organization.id = :organizationId and m.userId = :userId
            """)
    Optional<Role> findRole(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId);

    @Query("""
            select new com.talentforge.api.identity.MemberView(m.id, u.id, u.displayName, u.email, m.role, m.createdAt)
            from Membership m, UserAccount u
            where u.id = m.userId and m.organization.id = :organizationId
            order by lower(u.displayName), u.id
            """)
    List<MemberView> findMembers(@Param("organizationId") UUID organizationId);
}
