package com.talentforge.api.identity;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates one synthetic organization with a user per role when the {@code demo}
 * profile is active. The shared password comes from
 * {@code TALENTFORGE_DEMO_PASSWORD}; nothing is seeded without it.
 */
@Component
@Profile("demo")
public class DemoAccountsSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAccountsSeeder.class);

    static final String ORGANIZATION_SLUG = "northwind-talent";

    record DemoAccount(String email, String displayName, Role role) {
    }

    static final List<DemoAccount> ACCOUNTS = List.of(
            new DemoAccount("admin@northwind.example.org", "Avery Admin", Role.ADMIN),
            new DemoAccount("recruiter@northwind.example.org", "Riley Recruiter", Role.RECRUITER),
            new DemoAccount("manager@northwind.example.org", "Morgan Manager", Role.HIRING_MANAGER),
            new DemoAccount("interviewer@northwind.example.org", "Indy Interviewer", Role.INTERVIEWER));

    private final OrganizationRepository organizations;
    private final UserAccountRepository users;
    private final MembershipRepository memberships;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    public DemoAccountsSeeder(OrganizationRepository organizations, UserAccountRepository users,
            MembershipRepository memberships, PasswordEncoder passwordEncoder,
            @Value("${talentforge.demo.password:}") String password) {
        this.organizations = organizations;
        this.users = users;
        this.memberships = memberships;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password.isBlank()) {
            log.warn("demo profile active but TALENTFORGE_DEMO_PASSWORD is not set; skipping demo accounts");
            return;
        }
        Organization organization = organizations.findBySlug(ORGANIZATION_SLUG)
                .orElseGet(() -> organizations.save(new Organization("Northwind Talent (synthetic)", ORGANIZATION_SLUG)));
        int created = 0;
        for (DemoAccount account : ACCOUNTS) {
            if (users.findByEmail(account.email()).isPresent()) {
                continue;
            }
            UserAccount user = users.save(
                    new UserAccount(account.email(), account.displayName(), passwordEncoder.encode(password)));
            memberships.save(new Membership(organization, user, account.role()));
            created++;
        }
        log.info("demo accounts ready ({} created)", created);
    }
}
