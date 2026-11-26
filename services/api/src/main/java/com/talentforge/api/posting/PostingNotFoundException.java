package com.talentforge.api.posting;

import java.util.UUID;

/** No posting with this ID in the organization from the URL. */
public class PostingNotFoundException extends RuntimeException {

    public PostingNotFoundException(UUID id) {
        super("Job posting not found: " + id);
    }
}
