package com.talentforge.api.careers;

import java.time.OffsetDateTime;
import java.util.UUID;

/** An open posting as anyone on the careers site sees it. No internal fields (headcount, people, notes). */
public record PublicJob(
        UUID id,
        String title,
        String department,
        String location,
        String employmentType,
        Integer salaryMin,
        Integer salaryMax,
        String salaryCurrency,
        String description,
        OffsetDateTime publishedAt,
        PublicOrganization organization) {

    public record PublicOrganization(String name, String slug) {
    }
}
