package com.talentforge.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class AppUserDetailsServiceTest {

    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final AppUserDetailsService service = new AppUserDetailsService(users);

    @Test
    void looksUpByNormalizedEmail() {
        UserAccount account = new UserAccount("riley@example.org", "Riley Recruiter", "{noop}secret");
        when(users.findByEmail("riley@example.org")).thenReturn(Optional.of(account));

        UserDetails details = service.loadUserByUsername("  Riley@Example.ORG ");

        assertThat(details).isInstanceOf(AppUserDetails.class);
        assertThat(details.getUsername()).isEqualTo("riley@example.org");
        assertThat(details.getPassword()).isEqualTo("{noop}secret");
        assertThat(details.isEnabled()).isTrue();
        assertThat(((AppUserDetails) details).getId()).isEqualTo(account.getId());
    }

    @Test
    void reportsDisabledAccounts() {
        UserAccount account = new UserAccount("gone@example.org", "Gone User", "{noop}secret");
        account.disable();
        when(users.findByEmail("gone@example.org")).thenReturn(Optional.of(account));

        assertThat(service.loadUserByUsername("gone@example.org").isEnabled()).isFalse();
    }

    @Test
    void throwsForUnknownEmail() {
        when(users.findByEmail("nobody@example.org")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nobody@example.org"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void erasesPasswordHash() {
        AppUserDetails details = AppUserDetails.from(new UserAccount("a@example.org", "A", "{noop}secret"));
        details.eraseCredentials();
        assertThat(details.getPassword()).isNull();
    }
}
