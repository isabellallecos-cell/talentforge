package com.talentforge.api.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.talentforge.api.identity.MembershipRepository;
import com.talentforge.api.identity.Role;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizationAccessTest {

    private static final UUID USER = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID ORG_A = UUID.fromString("00000000-0000-4000-8000-0000000000aa");
    private static final UUID ORG_B = UUID.fromString("00000000-0000-4000-8000-0000000000bb");

    private final MembershipRepository memberships = mock(MembershipRepository.class);
    private final OrganizationAccess access = new OrganizationAccess(memberships);

    @BeforeEach
    void setUp() {
        when(memberships.findRole(ORG_A, USER)).thenReturn(Optional.of(Role.RECRUITER));
        when(memberships.findRole(ORG_B, USER)).thenReturn(Optional.empty());
    }

    @Test
    void returnsRoleWhenPermissionIsGranted() {
        assertThat(access.require(USER, ORG_A, Permission.CANDIDATES_MANAGE)).isEqualTo(Role.RECRUITER);
    }

    @Test
    void memberWithoutPermissionIsDenied() {
        assertThatThrownBy(() -> access.require(USER, ORG_A, Permission.REQUISITIONS_APPROVE))
                .isInstanceOf(PermissionDeniedException.class)
                .extracting("permission").isEqualTo(Permission.REQUISITIONS_APPROVE);
    }

    @Test
    void nonMemberGetsNotFoundEvenForHarmlessPermissions() {
        assertThatThrownBy(() -> access.require(USER, ORG_B, Permission.INTERVIEWS_VIEW_ASSIGNED))
                .isInstanceOf(OrganizationNotFoundException.class);
        assertThatThrownBy(() -> access.roleIn(USER, ORG_B))
                .isInstanceOf(OrganizationNotFoundException.class);
    }

    @Test
    void roleIsReadOnEveryCallSoChangesApplyImmediately() {
        when(memberships.findRole(ORG_A, USER)).thenReturn(Optional.of(Role.INTERVIEWER));

        assertThatThrownBy(() -> access.require(USER, ORG_A, Permission.CANDIDATES_MANAGE))
                .isInstanceOf(PermissionDeniedException.class);
    }
}
