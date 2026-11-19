package com.talentforge.api.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "memberships")
public class Membership {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Membership() {
    }

    public Membership(Organization organization, UserAccount user, Role role) {
        this.id = UUID.randomUUID();
        this.organization = organization;
        this.userId = user.getId();
        this.role = role;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public UUID getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }
}
