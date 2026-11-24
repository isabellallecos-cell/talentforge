package com.talentforge.api.organization;

import com.talentforge.api.access.OrganizationAccess;
import com.talentforge.api.access.Permission;
import com.talentforge.api.access.RolePermissions;
import com.talentforge.api.identity.AppUserDetails;
import com.talentforge.api.identity.MembershipRepository;
import com.talentforge.api.identity.Role;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/organizations/{organizationId}")
public class OrganizationController {

    private final OrganizationAccess access;
    private final MembershipRepository memberships;

    public OrganizationController(OrganizationAccess access, MembershipRepository memberships) {
        this.access = access;
        this.memberships = memberships;
    }

    /** The caller's role and permission keys, used by the web app to build its navigation. */
    @GetMapping("/access")
    public AccessResponse myAccess(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID organizationId) {
        Role role = access.roleIn(user.getId(), organizationId);
        List<String> permissions = RolePermissions.of(role).stream().map(Permission::key).sorted().toList();
        return new AccessResponse(organizationId, role.dbValue(), permissions);
    }

    @GetMapping("/members")
    public List<MemberResponse> members(@AuthenticationPrincipal AppUserDetails user,
            @PathVariable UUID organizationId) {
        access.require(user.getId(), organizationId, Permission.MEMBERS_VIEW);
        return memberships.findMembers(organizationId).stream().map(MemberResponse::from).toList();
    }
}
