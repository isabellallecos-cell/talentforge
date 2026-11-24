-- Week 3: job requisitions and their approval history.
--
-- Lifecycle (enforced in RequisitionWorkflow and backed by the CHECKs below):
--   draft -> pending_approval -> approved
--                             -> rejected -> draft (reopen)
--   pending_approval -> draft (withdraw)
--
-- Rollback: Flyway migrations are forward-only here. To undo, add a new
-- migration that drops requisition_events, its trigger function and
-- job_requisitions in that order.

CREATE TABLE job_requisitions (
    id                uuid         PRIMARY KEY,
    organization_id   uuid         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    title             varchar(200) NOT NULL,
    department        varchar(120) NOT NULL,
    location          varchar(120) NOT NULL,
    employment_type   varchar(32)  NOT NULL,
    headcount         integer      NOT NULL,
    salary_min        integer,
    salary_max        integer,
    salary_currency   varchar(3),
    description       varchar(4000),
    hiring_manager_id uuid         NOT NULL,
    status            varchar(32)  NOT NULL DEFAULT 'draft',
    created_by        uuid         NOT NULL REFERENCES app_users (id),
    submitted_by      uuid         REFERENCES app_users (id),
    submitted_at      timestamptz,
    decided_by        uuid         REFERENCES app_users (id),
    decided_at        timestamptz,
    decision_note     varchar(2000),
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_at        timestamptz  NOT NULL DEFAULT now(),
    version           bigint       NOT NULL DEFAULT 0,
    -- Lets requisition_events reference (organization_id, id) so an event can
    -- never point at another organization's requisition.
    CONSTRAINT job_requisitions_org_id_key UNIQUE (organization_id, id),
    -- The hiring manager must be a member of the same organization.
    CONSTRAINT job_requisitions_hiring_manager_fk FOREIGN KEY (organization_id, hiring_manager_id)
        REFERENCES memberships (organization_id, user_id),
    CONSTRAINT job_requisitions_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT job_requisitions_department_not_blank CHECK (btrim(department) <> ''),
    CONSTRAINT job_requisitions_location_not_blank CHECK (btrim(location) <> ''),
    CONSTRAINT job_requisitions_employment_type_check
        CHECK (employment_type IN ('full_time', 'part_time', 'contract', 'internship')),
    CONSTRAINT job_requisitions_headcount_check CHECK (headcount BETWEEN 1 AND 50),
    CONSTRAINT job_requisitions_salary_check CHECK (
        (salary_min IS NULL OR salary_min >= 0)
        AND (salary_max IS NULL OR salary_max >= 0)
        AND (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max)
        AND ((salary_min IS NULL AND salary_max IS NULL) = (salary_currency IS NULL))
        AND (salary_currency IS NULL OR salary_currency ~ '^[A-Z]{3}$')),
    CONSTRAINT job_requisitions_status_check
        CHECK (status IN ('draft', 'pending_approval', 'approved', 'rejected')),
    -- Everything past draft has been submitted by someone.
    CONSTRAINT job_requisitions_submitted_check CHECK (
        status = 'draft' OR (submitted_by IS NOT NULL AND submitted_at IS NOT NULL)),
    -- Decisions carry who and when; rejections also carry a reason.
    CONSTRAINT job_requisitions_decided_check CHECK (
        (status IN ('approved', 'rejected')) = (decided_by IS NOT NULL AND decided_at IS NOT NULL)),
    CONSTRAINT job_requisitions_rejection_note_check CHECK (
        status <> 'rejected' OR btrim(coalesce(decision_note, '')) <> ''),
    -- Four-eyes rule: nobody approves or rejects their own submission.
    CONSTRAINT job_requisitions_four_eyes_check CHECK (decided_by IS NULL OR decided_by <> submitted_by)
);

CREATE INDEX job_requisitions_org_status_idx ON job_requisitions (organization_id, status, updated_at DESC);
CREATE INDEX job_requisitions_hiring_manager_idx ON job_requisitions (organization_id, hiring_manager_id);

CREATE TABLE requisition_events (
    id               uuid          PRIMARY KEY,
    organization_id  uuid          NOT NULL,
    requisition_id   uuid          NOT NULL,
    actor_id         uuid          NOT NULL REFERENCES app_users (id),
    action           varchar(32)   NOT NULL,
    from_status      varchar(32),
    to_status        varchar(32)   NOT NULL,
    note             varchar(2000),
    created_at       timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT requisition_events_requisition_fk FOREIGN KEY (organization_id, requisition_id)
        REFERENCES job_requisitions (organization_id, id) ON DELETE CASCADE,
    CONSTRAINT requisition_events_action_check
        CHECK (action IN ('create', 'update', 'submit', 'withdraw', 'approve', 'reject', 'reopen')),
    CONSTRAINT requisition_events_from_status_check
        CHECK (from_status IS NULL OR from_status IN ('draft', 'pending_approval', 'approved', 'rejected')),
    CONSTRAINT requisition_events_to_status_check
        CHECK (to_status IN ('draft', 'pending_approval', 'approved', 'rejected')),
    CONSTRAINT requisition_events_create_check CHECK ((action = 'create') = (from_status IS NULL))
);

CREATE INDEX requisition_events_requisition_idx ON requisition_events (requisition_id, created_at);

-- The approval history is append-only: UPDATE is always rejected, DELETE only
-- when issued directly. Deletes cascading from the requisition or organization
-- run inside the foreign-key trigger (pg_trigger_depth() > 1) and are allowed.
CREATE FUNCTION requisition_events_append_only() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' AND pg_trigger_depth() > 1 THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'requisition_events is append-only'
        USING ERRCODE = 'integrity_constraint_violation';
END;
$$;

CREATE TRIGGER requisition_events_append_only
    BEFORE UPDATE OR DELETE ON requisition_events
    FOR EACH ROW EXECUTE FUNCTION requisition_events_append_only();
