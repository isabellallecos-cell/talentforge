package com.talentforge.api.requisition;

import static com.talentforge.api.requisition.RequisitionAction.APPROVE;
import static com.talentforge.api.requisition.RequisitionAction.REJECT;
import static com.talentforge.api.requisition.RequisitionAction.REOPEN;
import static com.talentforge.api.requisition.RequisitionAction.SUBMIT;
import static com.talentforge.api.requisition.RequisitionAction.UPDATE;
import static com.talentforge.api.requisition.RequisitionAction.WITHDRAW;
import static org.assertj.core.api.Assertions.assertThat;

import com.talentforge.api.identity.Role;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class RequisitionWorkflowTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-4000-8000-0000000000aa");
    private static final UUID RECRUITER = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID MANAGER = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID OTHER_MANAGER = UUID.fromString("00000000-0000-4000-8000-000000000003");
    private static final UUID ADMIN = UUID.fromString("00000000-0000-4000-8000-000000000004");
    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 3, 2, 9, 0, 0, 0, ZoneOffset.UTC);

    private static JobRequisition draft() {
        RequisitionDetails details = new RequisitionDetails("Backend Engineer", "Engineering", "Remote",
                EmploymentType.FULL_TIME, 1, null, null, null, null, MANAGER);
        return new JobRequisition(UUID.randomUUID(), ORG, details, RECRUITER, NOW);
    }

    private static JobRequisition pendingSubmittedBy(UUID submitter) {
        JobRequisition requisition = draft();
        requisition.moveTo(SUBMIT, RequisitionStatus.PENDING_APPROVAL, submitter, null, NOW);
        return requisition;
    }

    @ParameterizedTest
    @CsvSource({
            "DRAFT, UPDATE, DRAFT",
            "DRAFT, SUBMIT, PENDING_APPROVAL",
            "PENDING_APPROVAL, WITHDRAW, DRAFT",
            "PENDING_APPROVAL, APPROVE, APPROVED",
            "PENDING_APPROVAL, REJECT, REJECTED",
            "REJECTED, REOPEN, DRAFT"})
    void allowedTransitions(RequisitionStatus from, RequisitionAction action, RequisitionStatus to) {
        assertThat(RequisitionWorkflow.next(from, action)).contains(to);
    }

    @Test
    void everyOtherTransitionIsRejected() {
        int allowed = 0;
        for (RequisitionStatus from : RequisitionStatus.values()) {
            for (RequisitionAction action : RequisitionAction.values()) {
                if (RequisitionWorkflow.next(from, action).isPresent()) {
                    allowed++;
                }
            }
        }
        assertThat(allowed).isEqualTo(6);
        assertThat(RequisitionWorkflow.next(RequisitionStatus.APPROVED, REOPEN)).isEmpty();
        assertThat(RequisitionWorkflow.next(RequisitionStatus.DRAFT, APPROVE)).isEmpty();
        assertThat(RequisitionWorkflow.next(RequisitionStatus.PENDING_APPROVAL, UPDATE)).isEmpty();
    }

    @Test
    void assignedHiringManagerCanDecideSomeoneElsesSubmission() {
        JobRequisition requisition = pendingSubmittedBy(RECRUITER);

        assertThat(RequisitionWorkflow.allowedActions(requisition, MANAGER, Role.HIRING_MANAGER))
                .containsExactlyInAnyOrder(APPROVE, REJECT);
    }

    @Test
    void otherHiringManagersCannotDecide() {
        JobRequisition requisition = pendingSubmittedBy(RECRUITER);

        assertThat(RequisitionWorkflow.denialReason(requisition, APPROVE, OTHER_MANAGER, Role.HIRING_MANAGER))
                .hasValueSatisfying(reason -> assertThat(reason).contains("assigned hiring manager"));
        assertThat(RequisitionWorkflow.allowedActions(requisition, OTHER_MANAGER, Role.HIRING_MANAGER)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "HIRING_MANAGER"})
    void nobodyDecidesTheirOwnSubmission(Role role) {
        UUID submitter = role == Role.ADMIN ? ADMIN : MANAGER;
        JobRequisition requisition = pendingSubmittedBy(submitter);

        assertThat(RequisitionWorkflow.denialReason(requisition, APPROVE, submitter, role))
                .hasValueSatisfying(reason -> assertThat(reason).contains("you submitted"));
        assertThat(RequisitionWorkflow.denialReason(requisition, REJECT, submitter, role)).isPresent();
        assertThat(RequisitionWorkflow.allowedActions(requisition, submitter, role)).containsExactly(WITHDRAW);
    }

    @Test
    void adminsCanDecideAnyRequisitionTheyDidNotSubmit() {
        assertThat(RequisitionWorkflow.allowedActions(pendingSubmittedBy(RECRUITER), ADMIN, Role.ADMIN))
                .containsExactlyInAnyOrder(WITHDRAW, APPROVE, REJECT);
    }

    @Test
    void recruitersCannotApproveButCanWithdrawTheirOwnSubmission() {
        JobRequisition requisition = pendingSubmittedBy(RECRUITER);

        assertThat(RequisitionWorkflow.denialReason(requisition, APPROVE, RECRUITER, Role.RECRUITER))
                .hasValueSatisfying(reason -> assertThat(reason).contains("Your role"));
        assertThat(RequisitionWorkflow.allowedActions(requisition, RECRUITER, Role.RECRUITER))
                .containsExactly(WITHDRAW);
        UUID anotherRecruiter = UUID.randomUUID();
        assertThat(RequisitionWorkflow.allowedActions(requisition, anotherRecruiter, Role.RECRUITER)).isEmpty();
    }

    @Test
    void interviewersCannotTakeAnyAction() {
        for (JobRequisition requisition : new JobRequisition[] {draft(), pendingSubmittedBy(RECRUITER)}) {
            assertThat(RequisitionWorkflow.allowedActions(requisition, UUID.randomUUID(), Role.INTERVIEWER))
                    .isEmpty();
        }
    }

    @Test
    void draftsCanBeEditedAndSubmittedByCreators() {
        assertThat(RequisitionWorkflow.allowedActions(draft(), MANAGER, Role.HIRING_MANAGER))
                .isEqualTo(EnumSet.of(UPDATE, SUBMIT));
    }

    @Test
    void reopeningClearsSubmissionAndDecision() {
        JobRequisition requisition = pendingSubmittedBy(RECRUITER);
        requisition.moveTo(REJECT, RequisitionStatus.REJECTED, MANAGER, "Budget not approved", NOW);
        assertThat(requisition.getDecidedBy()).isEqualTo(MANAGER);
        assertThat(requisition.getDecisionNote()).isEqualTo("Budget not approved");

        requisition.moveTo(REOPEN, RequisitionStatus.DRAFT, RECRUITER, null, NOW.plusDays(1));

        assertThat(requisition.getStatus()).isEqualTo(RequisitionStatus.DRAFT);
        assertThat(requisition.getSubmittedBy()).isNull();
        assertThat(requisition.getDecidedBy()).isNull();
        assertThat(requisition.getDecisionNote()).isNull();
        assertThat(requisition.getUpdatedAt()).isEqualTo(NOW.plusDays(1));
    }
}
