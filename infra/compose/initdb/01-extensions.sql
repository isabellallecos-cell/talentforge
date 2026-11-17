-- Make pgvector available as soon as the database is created, so AC-01.3
-- can be checked before the API runs its Flyway migrations.
CREATE EXTENSION IF NOT EXISTS vector;
