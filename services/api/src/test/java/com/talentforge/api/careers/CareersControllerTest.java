package com.talentforge.api.careers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.talentforge.api.application.Applicant;
import com.talentforge.api.application.ApplicantRepository;
import com.talentforge.api.application.ApplicationRepository;
import com.talentforge.api.identity.Organization;
import com.talentforge.api.identity.OrganizationRepository;
import com.talentforge.api.posting.JobPosting;
import com.talentforge.api.posting.JobPostingRepository;
import com.talentforge.api.posting.PostingStatus;
import com.talentforge.api.requisition.EmploymentType;
import com.talentforge.api.requisition.JobRequisition;
import com.talentforge.api.requisition.RequisitionDetails;
import com.talentforge.api.security.SecurityConfig;
import com.talentforge.api.web.ApiExceptionHandler;
import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
 * The public careers API without a session, through the real
 * {@link CareersService}; repositories are in-memory and the two
 * {@code insertIfAbsent} queries mimic their ON CONFLICT behaviour.
 */
@WebMvcTest(CareersController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, CareersService.class, CareersControllerTest.FixedClock.class})
class CareersControllerTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-4000-8000-0000000000aa");
    private static final UUID OTHER_ORG = UUID.fromString("00000000-0000-4000-8000-0000000000bb");
    private static final UUID RECRUITER = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID MANAGER = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final Instant NOW = Instant.parse("2026-03-10T08:30:00Z");

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
    private OrganizationRepository organizations;

    @MockitoBean
    private JobPostingRepository postings;

    @MockitoBean
    private ApplicantRepository applicants;

    @MockitoBean
    private ApplicationRepository applications;

    private final Map<UUID, JobPosting> stored = new HashMap<>();
    private final Map<String, Applicant> applicantsByKey = new HashMap<>();
    private final Set<String> applied = new HashSet<>();
    private final List<String> coverLetters = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Organization org = organization(ORG, "Northwind Talent", "northwind-talent");
        Organization other = organization(OTHER_ORG, "Contoso Careers", "contoso");
        when(organizations.findBySlug(anyString())).thenReturn(Optional.empty());
        when(organizations.findBySlug("northwind-talent")).thenReturn(Optional.of(org));
        when(organizations.findBySlug("contoso")).thenReturn(Optional.of(other));
        when(postings.findByOrganizationIdAndId(any(), any())).thenAnswer(call -> find(call.getArgument(0),
                call.getArgument(1)));
        when(postings.findForApplication(any(), any())).thenAnswer(call -> find(call.getArgument(0),
                call.getArgument(1)));
        when(postings.findByOrganizationIdAndStatusOrderByPublishedAtDescIdAsc(any(), eq(PostingStatus.OPEN)))
                .thenAnswer(call -> stored.values().stream()
                        .filter(p -> p.getOrganizationId().equals(call.getArgument(0)) && p.isOpen())
                        .toList());
        when(applicants.insertIfAbsent(any(), any(), any(), any(), any(), any())).thenAnswer(call -> {
            String key = call.getArgument(1) + "|" + call.getArgument(2);
            if (applicantsByKey.containsKey(key)) {
                return 0;
            }
            applicantsByKey.put(key, applicant(call.getArgument(0), call.getArgument(1), call.getArgument(2),
                    call.getArgument(3), call.getArgument(4)));
            return 1;
        });
        when(applicants.findByOrganizationIdAndEmail(any(), any())).thenAnswer(call -> Optional
                .ofNullable(applicantsByKey.get(call.getArgument(0) + "|" + call.getArgument(1))));
        when(applications.insertIfAbsent(any(), any(), any(), any(), any(), any())).thenAnswer(call -> {
            if (!applied.add(call.getArgument(2) + "|" + call.getArgument(3))) {
                return 0;
            }
            coverLetters.add(call.getArgument(4));
            return 1;
        });
    }

    private Optional<JobPosting> find(UUID organizationId, UUID id) {
        return Optional.ofNullable(stored.get(id)).filter(p -> p.getOrganizationId().equals(organizationId));
    }

    private static Organization organization(UUID id, String name, String slug) {
        Organization organization = new Organization(name, slug);
        ReflectionTestUtils.setField(organization, "id", id);
        return organization;
    }

    private static Applicant applicant(UUID id, UUID organizationId, String email, String name, String phone) {
        try {
            Constructor<Applicant> constructor = Applicant.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Applicant applicant = constructor.newInstance();
            ReflectionTestUtils.setField(applicant, "id", id);
            ReflectionTestUtils.setField(applicant, "organizationId", organizationId);
            ReflectionTestUtils.setField(applicant, "email", email);
            ReflectionTestUtils.setField(applicant, "fullName", name);
            ReflectionTestUtils.setField(applicant, "phone", phone);
            return applicant;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private UUID posting(UUID organizationId, String title, boolean open) {
        RequisitionDetails details = new RequisitionDetails(title, "Engineering", "Remote (EU)",
                EmploymentType.FULL_TIME, 3, 52000, 64000, "EUR", "Synthetic opening.", MANAGER);
        JobRequisition requisition = new JobRequisition(UUID.randomUUID(), organizationId, details, RECRUITER,
                OffsetDateTime.of(2026, 3, 1, 9, 0, 0, 0, ZoneOffset.UTC));
        JobPosting posting = new JobPosting(UUID.randomUUID(), requisition, RECRUITER,
                OffsetDateTime.of(2026, 3, 9, 10, 0, 0, 0, ZoneOffset.UTC));
        if (!open) {
            ReflectionTestUtils.setField(posting, "status", PostingStatus.CLOSED);
        }
        stored.put(posting.getId(), posting);
        return posting.getId();
    }

    private static String application(String name, String email) {
        return """
                {"fullName": "%s", "email": "%s", "phone": "+34 600 000 000",
                 "coverLetter": "I build synthetic things.", "privacyConsent": true}
                """.formatted(name, email);
    }

    private static MockHttpServletRequestBuilder apply(String slug, UUID postingId, String body) {
        return post("/v1/public/organizations/{s}/jobs/{id}/applications", slug, postingId).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void anyoneListsOpenJobsWithoutInternalFields() throws Exception {
        posting(ORG, "Backend Engineer", true);
        posting(ORG, "Closed Role", false);
        posting(OTHER_ORG, "Other Company Role", true);

        mvc.perform(get("/v1/public/organizations/{s}/jobs", "northwind-talent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization.name").value("Northwind Talent"))
                .andExpect(jsonPath("$.jobs.length()").value(1))
                .andExpect(jsonPath("$.jobs[0].title").value("Backend Engineer"))
                .andExpect(jsonPath("$.jobs[0].employmentType").value("full_time"))
                .andExpect(jsonPath("$.jobs[0].salaryCurrency").value("EUR"))
                .andExpect(jsonPath("$.jobs[0].headcount").doesNotExist())
                .andExpect(jsonPath("$.jobs[0].hiringManager").doesNotExist())
                .andExpect(jsonPath("$.jobs[0].requisitionId").doesNotExist())
                .andExpect(jsonPath("$.jobs[0].publishedBy").doesNotExist());
    }

    @Test
    void unknownOrganizationsAndClosedOrForeignJobsAreNotFound() throws Exception {
        UUID closed = posting(ORG, "Closed Role", false);
        UUID foreign = posting(OTHER_ORG, "Other Company Role", true);

        mvc.perform(get("/v1/public/organizations/{s}/jobs", "nobody"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
        mvc.perform(get("/v1/public/organizations/{s}/jobs/{id}", "northwind-talent", closed))
                .andExpect(status().isNotFound());
        mvc.perform(get("/v1/public/organizations/{s}/jobs/{id}", "northwind-talent", foreign))
                .andExpect(status().isNotFound());
        mvc.perform(get("/v1/public/organizations/{s}/jobs/{id}", "contoso", foreign))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization.slug").value("contoso"));
        mvc.perform(get("/v1/public/organizations/{s}/jobs/{id}", "northwind-talent", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_parameter"));
    }

    @Test
    void anAnonymousVisitorApplies() throws Exception {
        UUID postingId = posting(ORG, "Backend Engineer", true);

        mvc.perform(apply("northwind-talent", postingId, application("  Ada Example ", " Ada@Example.ORG ")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("received"))
                .andExpect(jsonPath("$.jobTitle").value("Backend Engineer"))
                .andExpect(jsonPath("$.organizationName").value("Northwind Talent"));

        Applicant stored = applicantsByKey.get(ORG + "|ada@example.org");
        assertThat(stored).isNotNull();
        assertThat(stored.getFullName()).isEqualTo("Ada Example");
        assertThat(stored.getPhone()).isEqualTo("+34 600 000 000");
        assertThat(coverLetters).containsExactly("I build synthetic things.");
    }

    @Test
    void applyingTwiceGivesTheSameReceiptAndKeepsTheFirstApplication() throws Exception {
        UUID postingId = posting(ORG, "Backend Engineer", true);
        mvc.perform(apply("northwind-talent", postingId, application("Ada Example", "ada@example.org")));

        mvc.perform(apply("northwind-talent", postingId, application("Someone Else", "ADA@example.org")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("received"));

        assertThat(applied).hasSize(1);
        assertThat(applicantsByKey.get(ORG + "|ada@example.org").getFullName())
                .as("an anonymous request never overwrites an existing applicant")
                .isEqualTo("Ada Example");
    }

    @Test
    void theSameEmailIsASeparateApplicantInEachOrganization() throws Exception {
        mvc.perform(apply("northwind-talent", posting(ORG, "Backend Engineer", true),
                application("Ada Example", "ada@example.org"))).andExpect(status().isAccepted());
        mvc.perform(apply("contoso", posting(OTHER_ORG, "Data Analyst", true),
                application("Ada Example", "ada@example.org"))).andExpect(status().isAccepted());

        assertThat(applicantsByKey).containsOnlyKeys(ORG + "|ada@example.org", OTHER_ORG + "|ada@example.org");
    }

    @Test
    void closedJobsAndJobsOfAnotherOrganizationRefuseApplications() throws Exception {
        UUID closed = posting(ORG, "Closed Role", false);
        UUID foreign = posting(OTHER_ORG, "Other Company Role", true);

        mvc.perform(apply("northwind-talent", closed, application("Ada Example", "ada@example.org")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("This job is not open for applications."));
        mvc.perform(apply("northwind-talent", foreign, application("Ada Example", "ada@example.org")))
                .andExpect(status().isNotFound());
        verify(applicants, never()).insertIfAbsent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void invalidApplicationsListEveryField() throws Exception {
        UUID postingId = posting(ORG, "Backend Engineer", true);
        String invalid = """
                {"fullName": "Ada\\u0007", "email": "not-an-email", "phone": "call me",
                 "coverLetter": "%s", "privacyConsent": false}
                """.formatted("x".repeat(4001));

        mvc.perform(apply("northwind-talent", postingId, invalid))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fields.length()").value(5))
                .andExpect(jsonPath("$.fields[0].field").value("coverLetter"))
                .andExpect(jsonPath("$.fields[1].field").value("email"))
                .andExpect(jsonPath("$.fields[2].field").value("fullName"))
                .andExpect(jsonPath("$.fields[3].field").value("phone"))
                .andExpect(jsonPath("$.fields[4].field").value("privacyConsent"));
        mvc.perform(apply("northwind-talent", postingId, "{\"fullName\": \"Ada\", \"email\": \"ada@example.org\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields[0].field").value("privacyConsent"));
        verify(applicants, never()).insertIfAbsent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void applicationsNeedTheCsrfToken() throws Exception {
        UUID postingId = posting(ORG, "Backend Engineer", true);

        mvc.perform(post("/v1/public/organizations/{s}/jobs/{id}/applications", "northwind-talent", postingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(application("Ada Example", "ada@example.org")))
                .andExpect(status().isForbidden());
        verify(applicants, never()).insertIfAbsent(any(), any(), any(), any(), any(), any());
    }
}
