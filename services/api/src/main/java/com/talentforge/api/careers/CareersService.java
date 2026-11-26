package com.talentforge.api.careers;

import com.talentforge.api.application.Applicant;
import com.talentforge.api.application.ApplicantRepository;
import com.talentforge.api.application.ApplicationRepository;
import com.talentforge.api.identity.Organization;
import com.talentforge.api.identity.OrganizationRepository;
import com.talentforge.api.posting.JobPosting;
import com.talentforge.api.posting.JobPostingRepository;
import com.talentforge.api.posting.PostingStatus;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The public careers site: open postings per organization and anonymous
 * applications. Only open postings are visible; closed ones, unknown IDs and
 * unknown organizations all answer 404.
 */
@Service
public class CareersService {

    private static final Logger log = LoggerFactory.getLogger(CareersService.class);

    static final String RECEIVED = "received";

    private final OrganizationRepository organizations;
    private final JobPostingRepository postings;
    private final ApplicantRepository applicants;
    private final ApplicationRepository applications;
    private final Clock clock;

    public CareersService(OrganizationRepository organizations, JobPostingRepository postings,
            ApplicantRepository applicants, ApplicationRepository applications, Clock clock) {
        this.organizations = organizations;
        this.postings = postings;
        this.applicants = applicants;
        this.applications = applications;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PublicJobList openJobs(String slug) {
        Organization organization = organization(slug);
        PublicJob.PublicOrganization org = toPublic(organization);
        List<PublicJob> jobs = postings
                .findByOrganizationIdAndStatusOrderByPublishedAtDescIdAsc(organization.getId(), PostingStatus.OPEN)
                .stream()
                .map(p -> toPublic(p, org))
                .toList();
        return new PublicJobList(org, jobs);
    }

    @Transactional(readOnly = true)
    public PublicJob openJob(String slug, UUID postingId) {
        Organization organization = organization(slug);
        JobPosting posting = postings.findByOrganizationIdAndId(organization.getId(), postingId)
                .filter(JobPosting::isOpen)
                .orElseThrow(PublicJobNotFoundException::new);
        return toPublic(posting, toPublic(organization));
    }

    /**
     * Stores the applicant (first submission per organization + email wins)
     * and the application (first per applicant + posting wins). A repeat
     * returns the same receipt and changes nothing.
     */
    @Transactional
    public ApplicationReceipt apply(String slug, UUID postingId, ApplicationRequest request) {
        Organization organization = organization(slug);
        JobPosting posting = postings.findForApplication(organization.getId(), postingId)
                .filter(JobPosting::isOpen)
                .orElseThrow(PublicJobNotFoundException::new);
        OffsetDateTime now = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        applicants.insertIfAbsent(UUID.randomUUID(), organization.getId(), email, request.fullName().strip(),
                blankToNull(request.phone()), now);
        Applicant applicant = applicants.findByOrganizationIdAndEmail(organization.getId(), email).orElseThrow();
        UUID applicationId = UUID.randomUUID();
        boolean created = applications.insertIfAbsent(applicationId, organization.getId(), posting.getId(),
                applicant.getId(), blankToNull(request.coverLetter()), now) == 1;
        // IDs only: names, emails and cover letters never reach the logs.
        if (created) {
            log.info("application {} received for posting {} in organization {} from applicant {}", applicationId,
                    posting.getId(), organization.getId(), applicant.getId());
        } else {
            log.info("repeat application for posting {} in organization {} from applicant {} ignored",
                    posting.getId(), organization.getId(), applicant.getId());
        }
        return new ApplicationReceipt(RECEIVED, posting.getTitle(), organization.getName());
    }

    private Organization organization(String slug) {
        return organizations.findBySlug(slug).orElseThrow(PublicJobNotFoundException::new);
    }

    private static PublicJob.PublicOrganization toPublic(Organization organization) {
        return new PublicJob.PublicOrganization(organization.getName(), organization.getSlug());
    }

    private static PublicJob toPublic(JobPosting p, PublicJob.PublicOrganization organization) {
        return new PublicJob(p.getId(), p.getTitle(), p.getDepartment(), p.getLocation(),
                p.getEmploymentType().dbValue(), p.getSalaryMin(), p.getSalaryMax(), p.getSalaryCurrency(),
                p.getDescription(), p.getPublishedAt(), organization);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
