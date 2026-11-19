package com.talentforge.api.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    /** Looks up by normalized email; callers must pass {@link Emails#normalize(String)}. */
    Optional<UserAccount> findByEmail(String email);
}
