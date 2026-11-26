package com.talentforge.api.careers;

/** Unknown organization slug, unknown posting, or a closed posting: all look the same from outside. */
public class PublicJobNotFoundException extends RuntimeException {

    public PublicJobNotFoundException() {
        super("Job not found");
    }
}
