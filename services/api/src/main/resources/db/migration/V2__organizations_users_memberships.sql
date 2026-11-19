-- Identity tables for week 2: organizations, user accounts and per-organization
-- roles. Roles match packages/fixtures (admin, recruiter, hiring_manager,
-- interviewer).
--
-- Rollback: Flyway migrations are forward-only here. To undo, add a new
-- migration that drops memberships, app_users and organizations in that order.

CREATE TABLE organizations (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name        varchar(200) NOT NULL,
    slug        varchar(64)  NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT organizations_slug_key UNIQUE (slug),
    CONSTRAINT organizations_slug_format CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT organizations_name_not_blank CHECK (btrim(name) <> '')
);

CREATE TABLE app_users (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email          varchar(320) NOT NULL,
    display_name   varchar(200) NOT NULL,
    password_hash  varchar(255) NOT NULL,
    enabled        boolean      NOT NULL DEFAULT true,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT app_users_email_key UNIQUE (email),
    -- Emails are stored normalized so the unique key is case-insensitive.
    CONSTRAINT app_users_email_normalized CHECK (email = lower(btrim(email)) AND email <> ''),
    CONSTRAINT app_users_display_name_not_blank CHECK (btrim(display_name) <> '')
);

CREATE TABLE memberships (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  uuid        NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    user_id          uuid        NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    role             varchar(32) NOT NULL,
    created_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT memberships_organization_user_key UNIQUE (organization_id, user_id),
    CONSTRAINT memberships_role_check
        CHECK (role IN ('admin', 'recruiter', 'hiring_manager', 'interviewer'))
);

CREATE INDEX memberships_user_id_idx ON memberships (user_id);
