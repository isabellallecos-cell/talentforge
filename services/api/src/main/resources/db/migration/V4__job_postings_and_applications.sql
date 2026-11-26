-- Week 4: the public careers site. An approved requisition is published as a
-- job posting; anyone can read open postings and apply without an account.
--
--   job_requisitions (approved) --publish--> job_postings (open) --close--> closed
--   applicants (one per organization + email) --apply--> applications
--
-- A posting copies the requisition's public fields when it is published, so
-- what applicants saw does not change if the requisition is edited later.
--
-- Rollback: Flyway migrations are forward-only here. To undo, add a new
-- migration that drops applications, applicants and job_postings in that order.

CREATE TABLE job_postings (
    id               uuid          PRIMARY KEY,
    organization_id  uuid          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    requisition_id   uuid          NOT NULL,
    title            varchar(200)  NOT NULL,
    department       varchar(120)  NOT NULL,
    location         varchar(120)  NOT NULL,
    employment_type  varchar(32)   NOT NULL,
    salary_min       integer,
    salary_max       integer,
    salary_currency  varchar(3),
    description      varchar(4000),
    status           varchar(16)   NOT NULL DEFAULT 'open',
    published_by     uuid          NOT NULL REFERENCES app_users (id),
    published_at     timestamptz   NOT NULL,
    closed_by        uuid          REFERENCES app_users (id),
    closed_at        timestamptz,
    updated_at       timestamptz   NOT NULL DEFAULT now(),
    version          bigint        NOT NULL DEFAULT 0,
    CONSTRAINT job_postings_org_id_key UNIQUE (organization_id, id),
    -- One posting per requisition; publishing twice is refused, not duplicated.
    CONSTRAINT job_postings_requisition_key UNIQUE (requisition_id),
    CONSTRAINT job_postings_requisition_fk FOREIGN KEY (organization_id, requisition_id)
        REFERENCES job_requisitions (organization_id, id) ON DELETE CASCADE,
    CONSTRAINT job_postings_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT job_postings_employment_type_check
        CHECK (employment_type IN ('full_time', 'part_time', 'contract', 'internship')),
    CONSTRAINT job_postings_salary_check CHECK (
        (salary_min IS NULL OR salary_min >= 0)
        AND (salary_max IS NULL OR salary_max >= 0)
        AND (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max)
        AND ((salary_min IS NULL AND salary_max IS NULL) = (salary_currency IS NULL))),
    CONSTRAINT job_postings_status_check CHECK (status IN ('open', 'closed')),
    CONSTRAINT job_postings_closed_check
        CHECK ((status = 'closed') = (closed_by IS NOT NULL AND closed_at IS NOT NULL))
);

CREATE INDEX job_postings_org_status_idx ON job_postings (organization_id, status, published_at DESC);

CREATE TABLE applicants (
    id               uuid          PRIMARY KEY,
    organization_id  uuid          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    email            varchar(254)  NOT NULL,
    full_name        varchar(120)  NOT NULL,
    phone            varchar(40),
    created_at       timestamptz   NOT NULL DEFAULT now(),
    updated_at       timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT applicants_org_id_key UNIQUE (organization_id, id),
    -- Applicants are per organization: one organization never sees another's.
    CONSTRAINT applicants_org_email_key UNIQUE (organization_id, email),
    CONSTRAINT applicants_email_normalized CHECK (email = lower(btrim(email)) AND email <> ''),
    CONSTRAINT applicants_full_name_not_blank CHECK (btrim(full_name) <> '')
);

CREATE TABLE applications (
    id               uuid          PRIMARY KEY,
    organization_id  uuid          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    job_posting_id   uuid          NOT NULL,
    applicant_id     uuid          NOT NULL,
    status           varchar(32)   NOT NULL DEFAULT 'submitted',
    source           varchar(32)   NOT NULL DEFAULT 'careers_site',
    cover_letter     varchar(4000),
    consented_at     timestamptz   NOT NULL,
    submitted_at     timestamptz   NOT NULL,
    updated_at       timestamptz   NOT NULL DEFAULT now(),
    version          bigint        NOT NULL DEFAULT 0,
    -- Applying twice to the same posting keeps the first application.
    CONSTRAINT applications_posting_applicant_key UNIQUE (job_posting_id, applicant_id),
    CONSTRAINT applications_posting_fk FOREIGN KEY (organization_id, job_posting_id)
        REFERENCES job_postings (organization_id, id) ON DELETE CASCADE,
    CONSTRAINT applications_applicant_fk FOREIGN KEY (organization_id, applicant_id)
        REFERENCES applicants (organization_id, id) ON DELETE CASCADE,
    -- Pipeline stages arrive with the stage state machine.
    CONSTRAINT applications_status_check CHECK (status IN ('submitted')),
    CONSTRAINT applications_source_check CHECK (source IN ('careers_site'))
);

CREATE INDEX applications_org_posting_idx ON applications (organization_id, job_posting_id, submitted_at DESC);
CREATE INDEX applications_applicant_idx ON applications (organization_id, applicant_id);
