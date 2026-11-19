package com.talentforge.api.identity;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Authenticated principal kept in the HTTP session. Roles are scoped to an
 * organization, so the only global authority is {@code ROLE_USER}; organization
 * permissions are checked per request against {@code memberships}.
 */
public final class AppUserDetails implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final UUID id;
    private final String email;
    private final String displayName;
    private final boolean enabled;
    private String passwordHash;

    public AppUserDetails(UUID id, String email, String displayName, String passwordHash, boolean enabled) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
    }

    public static AppUserDetails from(UserAccount account) {
        return new AppUserDetails(account.getId(), account.getEmail(), account.getDisplayName(),
                account.getPasswordHash(), account.isEnabled());
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AUTHORITIES;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }
}
