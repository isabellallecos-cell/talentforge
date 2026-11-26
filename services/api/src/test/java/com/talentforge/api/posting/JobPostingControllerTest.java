package com.talentforge.api.posting;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.talentforge.api.access.OrganizationAccess;
import com.talentforge.api.application.ApplicationRepository;
import com.talentforge.api.identity.AppUserDetails;
import com.talentforge.api.identity.MembershipRepository;
import com.talentforge.api.identity.Organization;
import com.talentforge.api.identity.OrganizationRepository;
import com.talentforge.api.identity.Role;
import com.talentforge.api.identity.UserAccount;
import com.talentforge.api.identity.UserAccountRepository;
import com.talentforge.api.requisition.EmploymentType;
import com.talentforge.api.requisition.JobRequisition;
import com.talentforge.api.requisition.JobRequisitionRepository;
import com.talentforge.api.requisition.RequisitionDetails;
import com.talentforge.api.requisition.RequisitionStatus;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Publishing and closing postings through the real {@link JobPostingService}
 * and {@link OrganizationAccess}, with repositories replaced by in-memory maps.
 */
@WebMvcTest(JobPostingController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, OrganizationAccess.class, JobPostingService.class,
        JobPostingControllerTest.FixedClock.class})
class JobPostingControllerTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-4000-8000-0000000000aa");
    private static final UUID OTHER_ORG = UUID.fromString("00000000-0000-4000-8000-0000000000bb");
    private static final UUID ADMIN = UUID.fromString("00000000-0000-4000-8000-000000000009");
    private static final UUID RECRUITER = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID MANAGER = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID INTERVIEWER = UUID.fromString("00000000-0000-4000-8000-000000000005");
    private static final Instant NOW = Instant.parse("2026-03-09T10:00:00Z");

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
    private OrganizationRepository organizations;

    @MockitoBean
    private JobRequisitionRepository requisitions;

    @MockitoBean
    private JobPostingRepository postings;

    @MockitoBean
    private ApplicationRepository applications;

    private final Map<UUID, JobRequisition> storedRequisitions = new HashMap<>();
    private final Map<UUID, JobPosting> stored = new HashMap<>();

    @BeforeEach
    void setUp() {
        Map<UUID, Role> roles = Map.of(ADMIN, Role.ADMIN, RECRUITER, Role.RECRUITER, MANAGER, Role.HIRING_MANAGER,
                INTERVIEWER, Role.INTERVIEWER);
        when(memberships.findRole(any(), any())).thenAnswer(call -> ORG.equals(call.getArgument(0))
                ? Optional.ofNullable(roles.get(call.<UUID>getArgument(1)))
                : Optional.empty());
        Organization organization = new Organization("Northwind Talent", "northwind-talent");
        ReflectionTestUtils.setField(organization, "id", ORG);
        when(organizations.findById(ORG)).thenReturn(Optional.of(organization));
        when(users.findAllById(anyCollection())).thenAnswer(call -> {
            List<UserAccount> found = new ArrayList<>();
            for (Object id : call.<Iterable<?>>getArgument(0)) {
                UserAccount account = new UserAccount(id + "@example.org", "Name " + id.toString().substring(34), "x");
                ReflectionTestUtils.setField(account, "id", id);
                found.add(account);
            }
            return found;
        });
        when(requisitions.findByOrganizationIdAndId(any(), any())).thenAnswer(call -> Optional
                .ofNullable(storedRequisitions.get(call.<UUID>getArgument(1)))
                .filter(r -> r.getOrganizationId().equals(call.getArgument(0))));
        when(postings.saveAndFlush(any(JobPosting.class))).thenAnswer(call -> {
            JobPosting p = call.getArgument(0);
            Long version = p.getVersion();
            ReflectionTestUtils.setField(p, "version", version == null ? 0L : version + 1);
            stored.put(p.getId(), p);
            return p;
        });
        when(postings.findByOrganizationIdAndId(any(), any())).thenAnswer(call -> Optional
                .ofNullable(stored.get(call.<UUID>getArgument(1)))
                .filter(p -> p.getOrganizationId().equals(call.getArgument(0))));
        when(postings.findByOrganizationIdAndRequisitionId(any(), any())).thenAnswer(call -> stored.values().stream()
                .filter(p -> p.getOrganizationId().equals(call.getArgument(0))
                        && p.getRequisitionId().equals(call.getArgument(1)))
                .findFirst());
        when(postings.findByOrganizationIdOrderByPublishedAtDescIdAsc(ORG))
                .thenAnswer(call -> List.copyOf(stored.values()));
        when(applications.countByPosting(any(), anyCollection())).thenReturn(List.of());
    }

    private UUID requisition(RequisitionStatus status) {
        RequisitionDetails details = new RequisitionDetails("Backend Engineer", "Engineering", "Remote (EU)",
                EmploymentType.FULL_TIME, 2, 52000, 64000, "EUR", "Synthetic opening.", MANAGER);
        JobRequisition r = new JobRequisition(UUID.randomUUID(), ORG, details, RECRUITER,
                OffsetDateTime.of(2026, 3, 1, 9, 0, 0, 0, ZoneOffset.UTC));
        ReflectionTestUtils.setField(r, "status", status);
        storedRequisitions.put(r.getId(), r);
        return r.getId();
    }

    private static AppUserDetails as(UUID id) {
        return new AppUserDetails(id, "someone@example.org", "Someone", null, true);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder publish(UUID requisitionId) {
        return json(post("/v1/organizations/{o}/jobs", ORG), "{\"requisitionId\": \"%s\"}".formatted(requisitionId));
    }

    @Test
    void recruiterPublishesAnApprovedRequisition() throws Exception {
        UUID requisitionId = requisition(RequisitionStatus.APPROVED);

        mvc.perform(publish(requisitionId).with(user(as(RECRUITER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requisitionId").value(requisitionId.toString()))
                .andExpect(jsonPath("$.title").value("Backend Engineer"))
                .andExpect(jsonPath("$.status").value("open"))
                .andExpect(jsonPath("$.publishedBy.id").value(RECRUITER.toString()))
                .andExpect(jsonPath("$.publishedAt").value("2026-03-09T10:00:00Z"))
                .andExpect(jsonPath("$.applicationCount").value(0))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.publicPath").value(
                        "/careers/northwind-talent/jobs/" + stored.keySet().iterator().next()));

        JobPosting posting = stored.values().iterator().next();
        assertThat(posting.getSalaryCurrency()).isEqualTo("EUR");
        assertThat(posting.getOrganizationId()).isEqualTo(ORG);
    }

    @Test
    void onlyApprovedRequisitionsArePublished() throws Exception {
        for (RequisitionStatus status : List.of(RequisitionStatus.DRAFT, RequisitionStatus.PENDING_APPROVAL,
                RequisitionStatus.REJECTED)) {
            mvc.perform(publish(requisition(status)).with(user(as(RECRUITER))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("requisition_not_approved"));
        }
        verify(postings, never()).saveAndFlush(any());
    }

    @Test
    void publishingTwiceIsRefused() throws Exception {
        UUID requisitionId = requisition(RequisitionStatus.APPROVED);
        mvc.perform(publish(requisitionId).with(user(as(RECRUITER)))).andExpect(status().isCreated());

        mvc.perform(publish(requisitionId).with(user(as(ADMIN))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already_published"));
        assertThat(stored).hasSize(1);
    }

    @Test
    void aConcurrentPublishLosingTheUniqueKeyIsAConflict() throws Exception {
        UUID requisitionId = requisition(RequisitionStatus.APPROVED);
        when(postings.saveAndFlush(any(JobPosting.class)))
                .thenThrow(new DataIntegrityViolationException("job_postings_requisition_key"));

        mvc.perform(publish(requisitionId).with(user(as(RECRUITER))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already_published"));
    }

    @Test
    void hiringManagersAndInterviewersCannotPublish() throws Exception {
        UUID requisitionId = requisition(RequisitionStatus.APPROVED);
        for (UUID actor : List.of(MANAGER, INTERVIEWER)) {
            mvc.perform(publish(requisitionId).with(user(as(actor))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("forbidden"));
        }
    }

    @Test
    void otherOrganizationsRequisitionsLookMissing() throws Exception {
        UUID requisitionId = requisition(RequisitionStatus.APPROVED);

        mvc.perform(json(post("/v1/organizations/{o}/jobs", OTHER_ORG),
                        "{\"requisitionId\": \"%s\"}".formatted(requisitionId)).with(user(as(RECRUITER))))
                .andExpect(status().isNotFound());
        mvc.perform(publish(UUID.randomUUID()).with(user(as(RECRUITER))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Requisition not found."));
    }

    @Test
    void missingRequisitionIdIsAValidationError() throws Exception {
        mvc.perform(json(post("/v1/organizations/{o}/jobs", ORG), "{}").with(user(as(RECRUITER))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields[0].field").value("requisitionId"));
    }

    @Test
    void closingNeedsTheCurrentVersionAndHappensOnce() throws Exception {
        mvc.perform(publish(requisition(RequisitionStatus.APPROVED)).with(user(as(RECRUITER))));
        UUID postingId = stored.keySet().iterator().next();

        mvc.perform(json(post("/v1/organizations/{o}/jobs/{id}/close", ORG, postingId), "{\"version\": 5}")
                        .with(user(as(RECRUITER))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("stale_version"));
        mvc.perform(json(post("/v1/organizations/{o}/jobs/{id}/close", ORG, postingId), "{\"version\": 0}")
                        .with(user(as(RECRUITER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("closed"))
                .andExpect(jsonPath("$.closedBy.id").value(RECRUITER.toString()))
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(json(post("/v1/organizations/{o}/jobs/{id}/close", ORG, postingId), "{\"version\": 1}")
                        .with(user(as(RECRUITER))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("posting_closed"));
    }

    @Test
    void closingAnUnknownPostingIs404() throws Exception {
        mvc.perform(json(post("/v1/organizations/{o}/jobs/{id}/close", ORG, UUID.randomUUID()), "{\"version\": 0}")
                        .with(user(as(RECRUITER))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job posting not found."));
    }

    @Test
    void membersWhoSeeRequisitionsListPostingsWithCounts() throws Exception {
        UUID requisitionId = requisition(RequisitionStatus.APPROVED);
        mvc.perform(publish(requisitionId).with(user(as(RECRUITER))));
        UUID postingId = stored.keySet().iterator().next();
        when(applications.countByPosting(any(), anyCollection())).thenReturn(List.of(new ApplicationRepository
                .PostingCount() {
            @Override
            public UUID getPostingId() {
                return postingId;
            }

            @Override
            public long getTotal() {
                return 3;
            }
        }));

        mvc.perform(get("/v1/organizations/{o}/jobs", ORG).param("requisitionId", requisitionId.toString())
                        .with(user(as(MANAGER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].applicationCount").value(3));
        mvc.perform(get("/v1/organizations/{o}/jobs", ORG).param("requisitionId", UUID.randomUUID().toString())
                        .with(user(as(MANAGER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/v1/organizations/{o}/jobs", ORG).with(user(as(INTERVIEWER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void signInIsRequired() throws Exception {
        mvc.perform(get("/v1/organizations/{o}/jobs", ORG)).andExpect(status().isUnauthorized());
    }
}
