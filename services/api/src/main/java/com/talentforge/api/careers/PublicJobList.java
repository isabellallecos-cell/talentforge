package com.talentforge.api.careers;

import java.util.List;

/** {@code GET /v1/public/organizations/{slug}/jobs}: the organization and its open postings, newest first. */
public record PublicJobList(PublicJob.PublicOrganization organization, List<PublicJob> jobs) {
}
