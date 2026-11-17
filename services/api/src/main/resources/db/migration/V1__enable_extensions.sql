-- Extensions used by later migrations. pgvector stores note embeddings
-- for summary retrieval (ADR 0001).
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
