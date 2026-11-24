package com.talentforge.api.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.talentforge.api.identity.Role;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RolePermissionsTest {

    @ParameterizedTest
    @EnumSource(Role.class)
    void everyRoleHasAnEntry(Role role) {
        assertThat(RolePermissions.of(role)).isNotNull();
    }

    @Test
    void adminHasEveryPermission() {
        assertThat(RolePermissions.of(Role.ADMIN)).containsExactlyInAnyOrder(Permission.values());
    }

    @Test
    void onlyAdminsManageMembers() {
        assertThat(rolesWith(Permission.MEMBERS_MANAGE)).containsExactly(Role.ADMIN);
    }

    @Test
    void recruitersManageCandidatesButCannotApproveRequisitions() {
        Set<Permission> recruiter = RolePermissions.of(Role.RECRUITER);
        assertThat(recruiter).contains(Permission.CANDIDATES_MANAGE, Permission.REQUISITIONS_CREATE)
                .doesNotContain(Permission.REQUISITIONS_APPROVE, Permission.MEMBERS_MANAGE,
                        Permission.SCORECARDS_SUBMIT);
    }

    @Test
    void hiringManagersApproveRequisitions() {
        assertThat(rolesWith(Permission.REQUISITIONS_APPROVE))
                .containsExactlyInAnyOrder(Role.ADMIN, Role.HIRING_MANAGER);
    }

    @Test
    void interviewersOnlySeeTheirInterviewsAndSubmitScorecards() {
        assertThat(RolePermissions.of(Role.INTERVIEWER))
                .containsExactlyInAnyOrder(Permission.INTERVIEWS_VIEW_ASSIGNED, Permission.SCORECARDS_SUBMIT);
    }

    @Test
    void permissionSetsAreReadOnly() {
        assertThatThrownBy(() -> RolePermissions.of(Role.RECRUITER).add(Permission.MEMBERS_MANAGE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void permissionKeysAreUnique() {
        assertThat(Arrays.stream(Permission.values()).map(Permission::key)).doesNotHaveDuplicates();
    }

    private static Set<Role> rolesWith(Permission permission) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (Role role : Role.values()) {
            if (RolePermissions.allows(role, permission)) {
                roles.add(role);
            }
        }
        return roles;
    }
}
