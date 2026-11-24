package com.talentforge.api.requisition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.talentforge.api.access.OrganizationAccess;
import com.talentforge.api.identity.AppUserDetails;
import com.talentforge.api.identity.MemberView;
import com.talentforge.api.identity.MembershipRepository;
import com.talentforge.api.identity.Role;
import com.talentforge.api.identity.UserAccount;
import com.talentforge.api.identity.UserAccountRepository;
import com.talentforge.api.security.SecurityConfig;
import com.talentforge.api.web.ApiExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * HTTP contract of the requisition API through the real {@link RequisitionService}
 * and {@link OrganizationAccess}; only the repositories are replaced by an
 * in-memory map.
 */
@WebMvcTest(RequisitionController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, OrganizationAccess.class, RequisitionService.class,
        RequisitionControllerTest.FixedClock.class})
class RequisitionControllerTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-4000-8000-0000000000aa");
    private static final UUID OTHER_ORG = UUID.fromString("00000000-0000-4000-8000-0000000000bb");
    private static final UUID RECRUITER = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID MANAGER = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID OTHER_MANAGER = UUID.fromString("00000000-0000-4000-8000-000000000003");
    private static final UUID INTERVIEWER = UUID.fromString("00000000-0000-4000-8000-000000000005");
    private static final Instant NOW = Instant.parse("2026-03-02T09:00:00Z");

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private MembershipRepository memberships;

    @MockitoBean
    private UserAccountRepository users;

    @MockitoBean
    private JobRequisitionRepository requisitions;

    @MockitoBean
    private RequisitionEventRepository events;

    private final Map<UUID, JobRequisition> stored = new HashMap<>();
    private final List<RequisitionEvent> recorded = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Map<UUID, Role> roles = Map.of(RECRUITER, Role.RECRUITER, MANAGER, Role.HIRING_MANAGER,
                OTHER_MANAGER, Role.HIRING_MANAGER, INTERVIEWER, Role.INTERVIEWER);
        when(memberships.findRole(any(), any())).thenAnswer(call -> ORG.equals(call.getArgument(0))
                ? Optional.ofNullable(roles.get(call.<UUID>getArgument(1)))
                : Optional.empty());
        when(memberships.findMembers(ORG)).thenReturn(List.of(
                member(MANAGER, "Morgan Manager", Role.HIRING_MANAGER),
                member(RECRUITER, "Riley Recruiter", Role.RECRUITER),
                member(OTHER_MANAGER, "Quinn Manager", Role.HIRING_MANAGER)));
        when(users.findAllById(anyCollection())).thenAnswer(call -> {
            List<UserAccount> found = new ArrayList<>();
            for (Object id : call.<Iterable<?>>getArgument(0)) {
                UserAccount account = new UserAccount(id + "@example.org", "Name " + id.toString().substring(34), "x");
                ReflectionTestUtils.setField(account, "id", id);
                found.add(account);
            }
            return found;
        });
        // Mimics JPA: version starts at 0 and goes up on every save.
        when(requisitions.saveAndFlush(any(JobRequisition.class))).thenAnswer(call -> {
            JobRequisition r = call.getArgument(0);
            Long version = r.getVersion();
            ReflectionTestUtils.setField(r, "version", version == null ? 0L : version + 1);
            stored.put(r.getId(), r);
            return r;
        });
        when(requisitions.findByOrganizationIdAndId(any(), any())).thenAnswer(call -> Optional
                .ofNullable(stored.get(call.<UUID>getArgument(1)))
                .filter(r -> r.getOrganizationId().equals(call.getArgument(0))));
        when(requisitions.findByOrganizationIdOrderByUpdatedAtDescIdAsc(ORG))
                .thenAnswer(call -> List.copyOf(stored.values()));
        when(events.save(any(RequisitionEvent.class))).thenAnswer(call -> {
            recorded.add(call.getArgument(0));
            return call.getArgument(0);
        });
        when(events.findByOrganizationIdAndRequisitionIdOrderByCreatedAtAscIdAsc(any(), any()))
                .thenAnswer(call -> recorded.stream()
                        .filter(e -> e.getRequisitionId().equals(call.getArgument(1)))
                        .toList());
    }

    private static MemberView member(UUID id, String name, Role role) {
        return new MemberView(UUID.randomUUID(), id, name, "hidden@example.org", role,
                OffsetDateTime.of(2026, 1, 5, 9, 0, 0, 0, ZoneOffset.UTC));
    }

    private static AppUserDetails as(UUID id) {
        return new AppUserDetails(id, "someone@example.org", "Someone", null, true);
    }

    private static String body(UUID hiringManager, String title) {
        return """
                {"title": "%s", "department": "Engineering", "location": "Remote (EU)",
                 "employmentType": "full_time", "headcount": 2, "salaryMin": 52000, "salaryMax": 64000,
                 "salaryCurrency": "EUR", "description": "Synthetic opening.", "hiringManagerId": "%s"}
                """.formatted(title, hiringManager);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private UUID createDraft() throws Exception {
        mvc.perform(json(post("/v1/organizations/{o}/requisitions", ORG), body(MANAGER, "Backend Engineer"))
                        .with(user(as(RECRUITER))))
                .andExpect(status().isCreated());
        return stored.keySet().iterator().next();
    }

    private void act(UUID actor, UUID id, String action, long version, String note) throws Exception {
        String payload = note == null ? "{\"version\": %d}".formatted(version)
                : "{\"version\": %d, \"note\": \"%s\"}".formatted(version, note);
        mvc.perform(json(post("/v1/organizations/{o}/requisitions/{id}/{a}", ORG, id, action), payload)
                .with(user(as(actor))));
    }

    @Test
    void recruiterCreatesADraftAndItIsRecorded() throws Exception {
        mvc.perform(json(post("/v1/organizations/{o}/requisitions", ORG), body(MANAGER, "  Backend Engineer  "))
                        .with(user(as(RECRUITER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.title").value("Backend Engineer"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.hiringManager.id").value(MANAGER.toString()))
                .andExpect(jsonPath("$.hiringManager.displayName").value("Name 02"))
                .andExpect(jsonPath("$.createdBy.id").value(RECRUITER.toString()))
                .andExpect(jsonPath("$.createdAt").value("2026-03-02T09:00:00Z"))
                .andExpect(jsonPath("$.allowedActions[0]").value("update"))
                .andExpect(jsonPath("$.allowedActions[1]").value("submit"))
                .andExpect(jsonPath("$.hiringManager.email").doesNotExist());

        assertThat(recorded).singleElement().satisfies(event -> {
            assertThat(event.getAction()).isEqualTo(RequisitionAction.CREATE);
            assertThat(event.getFromStatus()).isNull();
            assertThat(event.getToStatus()).isEqualTo(RequisitionStatus.DRAFT);
        });
    }

    @Test
    void invalidFieldsAreListed() throws Exception {
        String invalid = """
                {"title": " ", "department": "Engineering", "location": "Remote",
                 "employmentType": "freelance", "headcount": 0, "hiringManagerId": "%s"}
                """.formatted(MANAGER);
        mvc.perform(json(post("/v1/organizations/{o}/requisitions", ORG), invalid).with(user(as(RECRUITER))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fields[0].field").value("employmentType"))
                .andExpect(jsonPath("$.fields[1].field").value("headcount"))
                .andExpect(jsonPath("$.fields[2].field").value("title"));
        verify(requisitions, never()).saveAndFlush(any());
    }

    @Test
    void salaryRangeAndHiringManagerAreChecked() throws Exception {
        String invalid = """
                {"title": "Data Analyst", "department": "Data", "location": "Lisbon",
                 "employmentType": "contract", "headcount": 1, "salaryMin": 70000, "salaryMax": 60000,
                 "hiringManagerId": "%s"}
                """.formatted(RECRUITER);
        mvc.perform(json(post("/v1/organizations/{o}/requisitions", ORG), invalid).with(user(as(RECRUITER))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields[0].field").value("salaryMax"))
                .andExpect(jsonPath("$.fields[1].field").value("salaryCurrency"))
                .andExpect(jsonPath("$.fields[2].field").value("hiringManagerId"));
    }

    @Test
    void interviewersCannotSeeOrCreateRequisitions() throws Exception {
        mvc.perform(get("/v1/organizations/{o}/requisitions", ORG).with(user(as(INTERVIEWER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(json(post("/v1/organizations/{o}/requisitions", ORG), body(MANAGER, "Designer"))
                        .with(user(as(INTERVIEWER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void otherOrganizationsLookMissing() throws Exception {
        UUID id = createDraft();

        mvc.perform(get("/v1/organizations/{o}/requisitions/{id}", OTHER_ORG, id).with(user(as(RECRUITER))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
        mvc.perform(get("/v1/organizations/{o}/requisitions/{id}", ORG, UUID.randomUUID())
                        .with(user(as(RECRUITER))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Requisition not found."));
    }

    @Test
    void fullApprovalFlowWithHistory() throws Exception {
        UUID id = createDraft();

        act(RECRUITER, id, "submit", 0, null);
        assertThat(stored.get(id).getStatus()).isEqualTo(RequisitionStatus.PENDING_APPROVAL);

        mvc.perform(get("/v1/organizations/{o}/requisitions/{id}", ORG, id).with(user(as(MANAGER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pending_approval"))
                .andExpect(jsonPath("$.submittedBy.id").value(RECRUITER.toString()))
                .andExpect(jsonPath("$.allowedActions.length()").value(2));

        mvc.perform(json(post("/v1/organizations/{o}/requisitions/{id}/approve", ORG, id),
                        "{\"version\": 1, \"note\": \"Budget confirmed\"}").with(user(as(MANAGER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"))
                .andExpect(jsonPath("$.decidedBy.id").value(MANAGER.toString()))
                .andExpect(jsonPath("$.decisionNote").value("Budget confirmed"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.allowedActions.length()").value(0));

        mvc.perform(get("/v1/organizations/{o}/requisitions/{id}/events", ORG, id).with(user(as(RECRUITER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].action").value("create"))
                .andExpect(jsonPath("$[1].action").value("submit"))
                .andExpect(jsonPath("$[1].fromStatus").value("draft"))
                .andExpect(jsonPath("$[2].action").value("approve"))
                .andExpect(jsonPath("$[2].toStatus").value("approved"))
                .andExpect(jsonPath("$[2].actor.id").value(MANAGER.toString()))
                .andExpect(jsonPath("$[2].note").value("Budget confirmed"));
    }

    @Test
    void rejectionNeedsAReasonAndCanBeReopened() throws Exception {
        UUID id = createDraft();
        act(RECRUITER, id, "submit", 0, null);

        mvc.perform(json(post("/v1/organizations/{o}/requisitions/{id}/reject", ORG, id), "{\"version\": 1}")
                        .with(user(as(MANAGER))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields[0].field").value("note"));

        act(MANAGER, id, "reject", 1, "Headcount frozen this quarter");
        assertThat(stored.get(id).getStatus()).isEqualTo(RequisitionStatus.REJECTED);

        mvc.perform(json(post("/v1/organizations/{o}/requisitions/{id}/reopen", ORG, id), "{\"version\": 2}")
                        .with(user(as(RECRUITER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.decisionNote").doesNotExist());
    }

    @Test
    void recruitersCannotApproveAndOtherManagersCannotDecide() throws Exception {
        UUID id = createDraft();
        act(RECRUITER, id, "submit", 0, null);

        mvc.perform(json(post("/v1/organizations/{o}/requisitions/{id}/approve", ORG, id), "{\"version\": 1}")
                        .with(user(as(RECRUITER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(json(post("/v1/organizations/{o}/requisitions/{id}/approve", ORG, id), "{\"version\": 1}")
                        .with(user(as(OTHER_MANAGER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("not_allowed"))
                .andExpect(jsonPath("$.message")
                        .value("Only the assigned hiring manager or an admin can approve this requisition."));
        assertThat(stored.get(id).getStatus()).isEqualTo(RequisitionStatus.PENDING_APPROVAL);
    }

    @Test
    void hiringManagersCannotApproveTheirOwnSubmission() throws Exception {
        mvc.perform(json(post("/v1/organizations/{o}/requisitions", ORG), body(MANAGER, "Product Designer"))
                        .with(user(as(MANAGER))))
                .andExpect(status().isCreated());
        UUID id = stored.keySet().iterator().next();
        act(MANAGER, id, "submit", 0, null);

        mvc.perform(json(post("/v1/organizations/{o}/requisitions/{id}/approve", ORG, id), "{\"version\": 1}")
                        .with(user(as(MANAGER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot approve a requisition you submitted."));
    }

    @Test
    void invalidTransitionsAndStaleVersionsConflict() throws Exception {
        UUID id = createDraft();

        mvc.perform(json(post("/v1/organizations/{o}/requisitions/{id}/approve", ORG, id), "{\"version\": 0}")
                        .with(user(as(MANAGER))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_transition"))
                .andExpect(jsonPath("$.message").value("Cannot approve a requisition that is draft."));

        act(RECRUITER, id, "submit", 0, null);

        mvc.perform(json(put("/v1/organizations/{o}/requisitions/{id}", ORG, id),
                        body(MANAGER, "Edited").replace("}", ", \"version\": 0}")).with(user(as(RECRUITER))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("stale_version"));
        mvc.perform(json(put("/v1/organizations/{o}/requisitions/{id}", ORG, id),
                        body(MANAGER, "Edited").replace("}", ", \"version\": 1}")).with(user(as(RECRUITER))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_transition"));
        assertThat(stored.get(id).getTitle()).isEqualTo("Backend Engineer");
    }

    @Test
    void draftsAreEditedWithTheCurrentVersion() throws Exception {
        UUID id = createDraft();

        mvc.perform(json(put("/v1/organizations/{o}/requisitions/{id}", ORG, id), body(OTHER_MANAGER, "Staff Engineer"))
                        .with(user(as(RECRUITER))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields[0].field").value("version"));
        mvc.perform(json(put("/v1/organizations/{o}/requisitions/{id}", ORG, id),
                        body(OTHER_MANAGER, "Staff Engineer").replace("}", ", \"version\": 0}"))
                        .with(user(as(RECRUITER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Staff Engineer"))
                .andExpect(jsonPath("$.hiringManager.id").value(OTHER_MANAGER.toString()))
                .andExpect(jsonPath("$.version").value(1));
        assertThat(recorded).extracting(RequisitionEvent::getAction)
                .containsExactly(RequisitionAction.CREATE, RequisitionAction.UPDATE);
    }

    @Test
    void unknownActionsAndStatusFiltersAreRejected() throws Exception {
        UUID id = createDraft();

        mvc.perform(json(post("/v1/organizations/{o}/requisitions/{id}/update", ORG, id), "{\"version\": 0}")
                        .with(user(as(RECRUITER))))
                .andExpect(status().isNotFound());
        mvc.perform(json(post("/v1/organizations/{o}/requisitions/{id}/publish", ORG, id), "{\"version\": 0}")
                        .with(user(as(RECRUITER))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/v1/organizations/{o}/requisitions", ORG).param("status", "open")
                        .with(user(as(RECRUITER))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_parameter"));
        mvc.perform(json(post("/v1/organizations/{o}/requisitions/{id}/submit", ORG, id), "{}")
                        .with(user(as(RECRUITER))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields[0].field").value("version"));
    }

    @Test
    void writesNeedCsrf() throws Exception {
        mvc.perform(post("/v1/organizations/{o}/requisitions", ORG).contentType(MediaType.APPLICATION_JSON)
                        .content(body(MANAGER, "Backend Engineer")).with(user(as(RECRUITER))))
                .andExpect(status().isForbidden());
        verify(requisitions, never()).saveAndFlush(any());
    }

    @Test
    void hiringManagerPickerListsEligibleMembersOnly() throws Exception {
        mvc.perform(get("/v1/organizations/{o}/requisitions/hiring-managers", ORG).with(user(as(RECRUITER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].displayName").value("Morgan Manager"))
                .andExpect(jsonPath("$[1].displayName").value("Quinn Manager"))
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    void listShowsPerCallerActions() throws Exception {
        UUID id = createDraft();
        act(RECRUITER, id, "submit", 0, null);

        mvc.perform(get("/v1/organizations/{o}/requisitions", ORG).with(user(as(OTHER_MANAGER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].allowedActions.length()").value(0));
        mvc.perform(get("/v1/organizations/{o}/requisitions", ORG).with(user(as(RECRUITER))))
                .andExpect(jsonPath("$[0].allowedActions[0]").value("withdraw"));
    }
}
