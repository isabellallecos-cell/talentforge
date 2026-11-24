package com.talentforge.api.organization;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.talentforge.api.access.OrganizationAccess;
import com.talentforge.api.identity.AppUserDetails;
import com.talentforge.api.identity.MemberView;
import com.talentforge.api.identity.MembershipRepository;
import com.talentforge.api.identity.Role;
import com.talentforge.api.security.SecurityConfig;
import com.talentforge.api.web.ApiExceptionHandler;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizationController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, OrganizationAccess.class})
class OrganizationControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID ORG_ID = UUID.fromString("00000000-0000-4000-8000-0000000000aa");
    private static final UUID OTHER_ORG_ID = UUID.fromString("00000000-0000-4000-8000-0000000000bb");
    private static final AppUserDetails CALLER =
            new AppUserDetails(USER_ID, "sam@example.org", "Sam Synthetic", null, true);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private MembershipRepository memberships;

    private void givenRole(Role role) {
        when(memberships.findRole(ORG_ID, USER_ID)).thenReturn(Optional.of(role));
        when(memberships.findMembers(ORG_ID)).thenReturn(List.of(new MemberView(
                UUID.fromString("00000000-0000-4000-8000-0000000000f1"), USER_ID, "Sam Synthetic",
                "sam@example.org", role, OffsetDateTime.of(2026, 1, 5, 9, 0, 0, 0, ZoneOffset.UTC))));
    }

    @Test
    void accessListsRoleAndSortedPermissionKeys() throws Exception {
        givenRole(Role.INTERVIEWER);

        mvc.perform(get("/v1/organizations/{id}/access", ORG_ID).with(user(CALLER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(ORG_ID.toString()))
                .andExpect(jsonPath("$.role").value("interviewer"))
                .andExpect(jsonPath("$.permissions.length()").value(2))
                .andExpect(jsonPath("$.permissions[0]").value("interviews:view-assigned"))
                .andExpect(jsonPath("$.permissions[1]").value("scorecards:submit"));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "RECRUITER"})
    void adminsAndRecruitersSeeMembers(Role role) throws Exception {
        givenRole(role);

        mvc.perform(get("/v1/organizations/{id}/members", ORG_ID).with(user(CALLER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("Sam Synthetic"))
                .andExpect(jsonPath("$[0].role").value(role.dbValue()))
                .andExpect(jsonPath("$[0].joinedAt").value("2026-01-05T09:00:00Z"));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"HIRING_MANAGER", "INTERVIEWER"})
    void otherRolesAreForbiddenFromMembers(Role role) throws Exception {
        givenRole(role);

        mvc.perform(get("/v1/organizations/{id}/members", ORG_ID).with(user(CALLER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        verify(memberships, never()).findMembers(ORG_ID);
    }

    @Test
    void nonMembersGet404ForAnotherOrganization() throws Exception {
        givenRole(Role.ADMIN);
        when(memberships.findRole(OTHER_ORG_ID, USER_ID)).thenReturn(Optional.empty());

        mvc.perform(get("/v1/organizations/{id}/members", OTHER_ORG_ID).with(user(CALLER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
        mvc.perform(get("/v1/organizations/{id}/access", OTHER_ORG_ID).with(user(CALLER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
        verify(memberships, never()).findMembers(OTHER_ORG_ID);
    }

    @Test
    void anonymousCallersGet401() throws Exception {
        mvc.perform(get("/v1/organizations/{id}/access", ORG_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
    }

    @Test
    void malformedOrganizationIdIsNotAServerError() throws Exception {
        mvc.perform(get("/v1/organizations/not-a-uuid/access").with(user(CALLER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_parameter"));
    }
}
