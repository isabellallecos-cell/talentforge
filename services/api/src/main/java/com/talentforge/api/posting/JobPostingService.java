package com.talentforge.api.posting;

import com.talentforge.api.access.OrganizationAccess;
import com.talentforge.api.access.Permission;
import com.talentforge.api.application.ApplicationRepository;
import com.talentforge.api.identity.Organization;
import com.talentforge.api.identity.OrganizationRepository;
import com.talentforge.api.identity.UserAccount;
import com.talentforge.api.identity.UserAccountRepository;
import com.talentforge.api.requisition.JobRequisition;
import com.talentforge.api.requisition.JobRequisitionRepository;
import com.talentforge.api.requisition.PersonRef;
import com.talentforge.api.requisition.RequisitionNotFoundException;
import com.talentforge.api.requisition.RequisitionStatus;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publishes approved requisitions to the careers site and closes postings.
 * Listing needs {@code requisitions:view}; publishing and closing need
 * {@code jobs:publish}.
 */
@Service
public class JobPostingService {

    private static final Logger log = LoggerFactory.getLogger(JobPostingService.class);

    private final OrganizationAccess access;
    private final JobPostingRepository postings;
    private final JobRequisitionRepository requisitions;
    private final ApplicationRepository applications;
    private final OrganizationRepository organizations;
    private final UserAccountRepository users;
    private final Clock clock;

    public JobPostingService(OrganizationAccess access, JobPostingRepository postings,
            JobRequisitionRepository requisitions, ApplicationRepository applications,
            OrganizationRepository organizations, UserAccountRepository users, Clock clock) {
        this.access = access;
        this.postings = postings;
        this.requisitions = requisitions;
        this.applications = applications;
        this.organizations = organizations;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<JobPostingResponse> list(UUID userId, UUID organizationId, Optional<UUID> requisitionId) {
        access.require(userId, organizationId, Permission.REQUISITIONS_VIEW);
        List<JobPosting> found = requisitionId
                .map(rid -> postings.findByOrganizationIdAndRequisitionId(organizationId, rid).stream().toList())
                .orElseGet(() -> postings.findByOrganizationIdOrderByPublishedAtDescIdAsc(organizationId));
        return toResponses(organizationId, found);
    }

    @Transactional
    public JobPostingResponse publish(UUID userId, UUID organizationId, UUID requisitionId) {
        access.require(userId, organizationId, Permission.JOBS_PUBLISH);
        JobRequisition requisition = requisitions.findByOrganizationIdAndId(organizationId, requisitionId)
                .orElseThrow(() -> new RequisitionNotFoundException(requisitionId));
        if (requisition.getStatus() != RequisitionStatus.APPROVED) {
            throw new PostingConflictException("requisition_not_approved",
                    "Only approved requisitions can be published.");
        }
        if (postings.findByOrganizationIdAndRequisitionId(organizationId, requisitionId).isPresent()) {
            throw alreadyPublished();
        }
        JobPosting posting;
        try {
            posting = postings.saveAndFlush(new JobPosting(UUID.randomUUID(), requisition, userId, now()));
        } catch (DataIntegrityViolationException e) {
            // A concurrent publish of the same requisition won the unique key.
            throw alreadyPublished();
        }
        log.info("posting {} published from requisition {} by user {} in organization {}", posting.getId(),
                requisitionId, userId, organizationId);
        return toResponses(organizationId, List.of(posting)).getFirst();
    }

    @Transactional
    public JobPostingResponse close(UUID userId, UUID organizationId, UUID postingId, long version) {
        access.require(userId, organizationId, Permission.JOBS_PUBLISH);
        JobPosting posting = postings.findByOrganizationIdAndId(organizationId, postingId)
                .orElseThrow(() -> new PostingNotFoundException(postingId));
        if (!Objects.equals(posting.getVersion(), version)) {
            throw new PostingConflictException("stale_version",
                    "Someone else changed this posting. Reload it and try again.");
        }
        if (!posting.isOpen()) {
            throw new PostingConflictException("posting_closed", "This posting is already closed.");
        }
        posting.close(userId, now());
        posting = postings.saveAndFlush(posting);
        log.info("posting {} closed by user {} in organization {}", postingId, userId, organizationId);
        return toResponses(organizationId, List.of(posting)).getFirst();
    }

    private static PostingConflictException alreadyPublished() {
        return new PostingConflictException("already_published", "This requisition is already published.");
    }

    private List<JobPostingResponse> toResponses(UUID organizationId, List<JobPosting> found) {
        if (found.isEmpty()) {
            return List.of();
        }
        String slug = organizations.findById(organizationId).map(Organization::getSlug).orElseThrow();
        Map<UUID, Long> counts = applications
                .countByPosting(organizationId, found.stream().map(JobPosting::getId).toList()).stream()
                .collect(Collectors.toMap(ApplicationRepository.PostingCount::getPostingId,
                        ApplicationRepository.PostingCount::getTotal));
        Set<UUID> people = new HashSet<>();
        found.forEach(p -> {
            people.add(p.getPublishedBy());
            people.add(p.getClosedBy());
        });
        Map<UUID, String> names = namesFor(people);
        return found.stream()
                .map(p -> new JobPostingResponse(p.getId(), p.getRequisitionId(), p.getTitle(), p.getDepartment(),
                        p.getLocation(), p.getEmploymentType().dbValue(), p.getStatus().dbValue(),
                        person(p.getPublishedBy(), names), p.getPublishedAt(), person(p.getClosedBy(), names),
                        p.getClosedAt(), counts.getOrDefault(p.getId(), 0L),
                        "/careers/" + slug + "/jobs/" + p.getId(), p.getVersion()))
                .toList();
    }

    private Map<UUID, String> namesFor(Collection<UUID> ids) {
        Set<UUID> wanted = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (wanted.isEmpty()) {
            return Map.of();
        }
        return users.findAllById(wanted).stream()
                .collect(Collectors.toMap(UserAccount::getId, UserAccount::getDisplayName, (a, b) -> a));
    }

    private static PersonRef person(UUID id, Map<UUID, String> names) {
        return id == null ? null : new PersonRef(id, names.get(id));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
