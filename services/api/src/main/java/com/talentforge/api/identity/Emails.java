package com.talentforge.api.identity;

import java.util.Locale;

/** Email normalization shared by login and account creation. */
public final class Emails {

    private Emails() {
    }

    public static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
