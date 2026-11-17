"""Synthetic hiring data for TalentForge development, tests and demos."""

from .generator import (
    ALLOWED_RESUME_TYPES,
    DEFAULT_REFERENCE_DATE,
    DEFAULT_SEED,
    EDGE_CASE_CHECKS,
    FIXTURE_VERSION,
    MAX_RESUME_BYTES,
    PROTECTED_FIELDS,
    ROLES,
    STAGES,
    TABLES,
    generate,
    validate,
    write_fixture_set,
)

__all__ = [
    "ALLOWED_RESUME_TYPES", "DEFAULT_REFERENCE_DATE", "DEFAULT_SEED", "EDGE_CASE_CHECKS",
    "FIXTURE_VERSION", "MAX_RESUME_BYTES", "PROTECTED_FIELDS", "ROLES", "STAGES", "TABLES",
    "generate", "validate", "write_fixture_set",
]
