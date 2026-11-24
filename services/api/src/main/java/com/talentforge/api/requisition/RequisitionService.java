package com.talentforge.api.requisition;

import com.talentforge.api.access.OrganizationAccess;
import com.talentforge.api.access.Permission;
import com.talentforge.api.identity.MemberView;
import com.talentforge.api.identity.MembershipRepository;
import com.talentforge.api.identity.Role;
import com.talentforge.api.identity.UserAccount;
import com.talentforge.api.identity.UserAccountRepository;
import com.talentforge.api.web.ApiError.FieldIssue;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, edits and moves job requisitions through approval. Every call
 * resolves the caller's role from {@code memberships} first (404 for
 * non-members, 403 without the permission), then applies
 * {@link RequisitionWorkflow}. Each change writes one {@link RequisitionEvent}
 * in the same transaction.
 */
@Service
public class RequisitionService {

    private static final Logger log = LoggerFactory.getLogger(RequisitionService.class);

    private static final Set<Role> HIRING_MANAGER_ROLES = Set.of(Role.HIRING_MANAGER, Role.ADMIN);

    private final OrganizationAccess access;
    private final JobRequisitionRepository requisitions;
    private final RequisitionEventRepository events;
    private final MembershipRepository memberships;
    private final UserAccountRepository users;
    private final Clock clock;

    public RequisitionService(OrganizationAccess access, JobRequisitionRepository requisitions,
            RequisitionEventRepository events, MembershipRepository memberships, UserAccountRepository users,
            Clock clock) {
        this.access = access;
        this.requisitions = requisitions;
        this.events = events;
        this.memberships = memberships;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RequisitionResponse> list(UUID userId, UUID organizationId, Optional<RequisitionStatus> status) {
        Role role = access.require(userId, organizationId, Permission.REQUISITIONS_VIEW);
        List<JobRequisition> found = status
                .map(s -> requisitions.findByOrganizationIdAndStatusOrderByUpdatedAtDescIdAsc(organizationId, s))
                .orElseGet(() -> requisitions.findByOrganizationIdOrderByUpdatedAtDescIdAsc(organizationId));
        Map<UUID, String> names = displayNames(found);
        return found.stream().map(r -> toResponse(r, userId, role, names)).toList();
    }

    @Transactional(readOnly = true)
    public RequisitionResponse get(UUID userId, UUID organizationId, UUID requisitionId) {
        Role role = access.require(userId, organizationId, Permission.REQUISITIONS_VIEW);
        JobRequisition requisition = load(organizationId, requisitionId);
        return toResponse(requisition, userId, role, displayNames(List.of(requisition)));
    }

    @Transactional
    public RequisitionResponse create(UUID userId, UUID organizationId, RequisitionRequest request) {
        Role role = access.require(userId, organizationId, Permission.REQUISITIONS_CREATE);
        RequisitionDetails details = validate(organizationId, request);
        OffsetDateTime now = now();
        JobRequisition requisition = requisitions.saveAndFlush(
                new JobRequisition(UUID.randomUUID(), organizationId, details, userId, now));
        record(requisition, userId, RequisitionAction.CREATE, null, null, now);
        return toResponse(requisition, userId, role, displayNames(List.of(requisition)));
    }

    @Transactional
    public RequisitionResponse update(UUID userId, UUID organizationId, UUID requisitionId,
            RequisitionRequest request) {
        Role role = access.require(userId, organizationId, Permission.REQUISITIONS_CREATE);
        JobRequisition requisition = load(organizationId, requisitionId);
        if (request.version() == null) {
            throw new RequisitionValidationException(List.of(new FieldIssue("version", "must not be null")));
        }
        checkVersion(requisition, request.version());
        RequisitionStatus from = requisition.getStatus();
        RequisitionWorkflow.next(from, RequisitionAction.UPDATE)
                .orElseThrow(() -> new InvalidTransitionException(from, RequisitionAction.UPDATE));
        requireAllowed(requisition, RequisitionAction.UPDATE, userId, role);
        RequisitionDetails details = validate(organizationId, request);
        OffsetDateTime now = now();
        requisition.apply(details, now);
        requisition = requisitions.saveAndFlush(requisition);
        record(requisition, userId, RequisitionAction.UPDATE, from, null, now);
        return toResponse(requisition, userId, role, displayNames(List.of(requisition)));
    }

    /** Applies a status change: submit, withdraw, approve, reject or reopen. */
    @Transactional
    public RequisitionResponse transition(UUID userId, UUID organizationId, UUID requisitionId,
            RequisitionAction action, ActionRequest request) {
        if (!RequisitionWorkflow.STATUS_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("Not a status action: " + action);
        }
        Role role = access.require(userId, organizationId, RequisitionWorkflow.permissionFor(action));
        JobRequisition requisition = load(organizationId, requisitionId);
        checkVersion(requisition, request.version());
        RequisitionStatus from = requisition.getStatus();
        RequisitionStatus to = RequisitionWorkflow.next(from, action)
                .orElseThrow(() -> new InvalidTransitionException(from, action));
        requireAllowed(requisition, action, userId, role);
        String note = blankToNull(request.note());
        if (action == RequisitionAction.REJECT && note == null) {
            throw new RequisitionValidationException(
                    List.of(new FieldIssue("note", "a reason is required to reject a requisition")));
        }
        OffsetDateTime now = now();
        requisition.moveTo(action, to, userId, note, now);
        requisition = requisitions.saveAndFlush(requisition);
        record(requisition, userId, action, from, note, now);
        return toResponse(requisition, userId, role, displayNames(List.of(requisition)));
    }

    @Transactional(readOnly = true)
    public List<RequisitionEventResponse> history(UUID userId, UUID organizationId, UUID requisitionId) {
        access.require(userId, organizationId, Permission.REQUISITIONS_VIEW);
        load(organizationId, requisitionId);
        List<RequisitionEvent> found =
                events.findByOrganizationIdAndRequisitionIdOrderByCreatedAtAscIdAsc(organizationId, requisitionId);
        Map<UUID, String> names = namesFor(found.stream().map(RequisitionEvent::getActorId).toList());
        return found.stream()
                .map(e -> new RequisitionEventResponse(e.getId(), e.getAction().dbValue(),
                        e.getFromStatus() == null ? null : e.getFromStatus().dbValue(), e.getToStatus().dbValue(),
                        person(e.getActorId(), names), e.getNote(), e.getCreatedAt()))
                .toList();
    }

    /** Members who can be named as a requisition's hiring manager (for the form's picker). */
    @Transactional(readOnly = true)
    public List<PersonRef> hiringManagerOptions(UUID userId, UUID organizationId) {
        access.require(userId, organizationId, Permission.REQUISITIONS_CREATE);
        return memberships.findMembers(organizationId).stream()
                .filter(member -> HIRING_MANAGER_ROLES.contains(member.role()))
                .map(member -> new PersonRef(member.userId(), member.displayName()))
                .toList();
    }

    private JobRequisition load(UUID organizationId, UUID requisitionId) {
        return requisitions.findByOrganizationIdAndId(organizationId, requisitionId)
                .orElseThrow(() -> new RequisitionNotFoundException(requisitionId));
    }

    private static void checkVersion(JobRequisition requisition, long expected) {
        if (!Objects.equals(requisition.getVersion(), expected)) {
            throw new StaleVersionException(requisition.getVersion());
        }
    }

    private static void requireAllowed(JobRequisition requisition, RequisitionAction action, UUID userId, Role role) {
        Optional<String> denial = RequisitionWorkflow.denialReason(requisition, action, userId, role);
        if (denial.isPresent()) {
            // IDs only: titles and notes stay out of the logs.
            log.info("requisition {} {} denied for user {} with role {} in organization {}",
                    requisition.getId(), action.dbValue(), userId, role.dbValue(), requisition.getOrganizationId());
            throw new ActionNotAllowedException(denial.get());
        }
    }

    private RequisitionDetails validate(UUID organizationId, RequisitionRequest request) {
        List<FieldIssue> issues = new ArrayList<>();
        Integer min = request.salaryMin();
        Integer max = request.salaryMax();
        String currency = blankToNull(request.salaryCurrency());
        if (min != null && max != null && min > max) {
            issues.add(new FieldIssue("salaryMax", "must be greater than or equal to salaryMin"));
        }
        if ((min != null || max != null) && currency == null) {
            issues.add(new FieldIssue("salaryCurrency", "is required when a salary is given"));
        }
        if (min == null && max == null && currency != null) {
            issues.add(new FieldIssue("salaryCurrency", "needs salaryMin or salaryMax"));
        }
        boolean eligible = memberships.findRole(organizationId, request.hiringManagerId())
                .filter(HIRING_MANAGER_ROLES::contains)
                .isPresent();
        if (!eligible) {
            issues.add(new FieldIssue("hiringManagerId", "must be a hiring manager or admin in this organization"));
        }
        if (!issues.isEmpty()) {
            throw new RequisitionValidationException(issues);
        }
        return new RequisitionDetails(request.title().strip(), request.department().strip(),
                request.location().strip(), EmploymentType.fromDbValue(request.employmentType()),
                request.headcount(), min, max, currency, blankToNull(request.description()),
                request.hiringManagerId());
    }

    private void record(JobRequisition requisition, UUID actorId, RequisitionAction action, RequisitionStatus from,
            String note, OffsetDateTime now) {
        events.save(new RequisitionEvent(requisition, actorId, action, from, note, now));
        log.info("requisition {} {}: {} -> {} by user {} in organization {}", requisition.getId(), action.dbValue(),
                from == null ? "-" : from.dbValue(), requisition.getStatus().dbValue(), actorId,
                requisition.getOrganizationId());
    }

    private RequisitionResponse toResponse(JobRequisition r, UUID userId, Role role, Map<UUID, String> names) {
        List<String> allowed = RequisitionWorkflow.allowedActions(r, userId, role).stream()
                .map(RequisitionAction::dbValue)
                .toList();
        return new RequisitionResponse(r.getId(), r.getOrganizationId(), r.getTitle(), r.getDepartment(),
                r.getLocation(), r.getEmploymentType().dbValue(), r.getHeadcount(), r.getSalaryMin(),
                r.getSalaryMax(), r.getSalaryCurrency(), r.getDescription(), person(r.getHiringManagerId(), names),
                r.getStatus().dbValue(), person(r.getCreatedBy(), names), person(r.getSubmittedBy(), names),
                r.getSubmittedAt(), person(r.getDecidedBy(), names), r.getDecidedAt(), r.getDecisionNote(),
                r.getCreatedAt(), r.getUpdatedAt(), r.getVersion(), allowed);
    }

    private Map<UUID, String> displayNames(Collection<JobRequisition> found) {
        Set<UUID> ids = new HashSet<>();
        for (JobRequisition r : found) {
            ids.add(r.getHiringManagerId());
            ids.add(r.getCreatedBy());
            ids.add(r.getSubmittedBy());
            ids.add(r.getDecidedBy());
        }
        return namesFor(ids);
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
        // PostgreSQL keeps microseconds; truncate so responses match what a reload returns.
        return OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
