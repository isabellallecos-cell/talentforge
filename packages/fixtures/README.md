# talentforge-fixtures

Deterministic synthetic hiring data for development, tests and the demo, following
[ADR 0001](../../docs/adr/0001-angular-spring-workspace-and-synthetic-data.md).
Python 3.11+, standard library only.

```bash
cd packages/fixtures
python3 -m talentforge_fixtures --out ../../.fixtures             # default seed 2207, reference date 2026-02-02
python3 -m talentforge_fixtures --out /tmp/fx --seed 7 --no-edge-cases
python3 -m unittest discover -s tests -t .                         # acceptance tests AC-01.6 to AC-01.15
```

## What the default set contains

- 3 organizations, each with an admin, 2 recruiters, 2 hiring managers and 3 interviewers
- 4 jobs per organization (3 open, 1 closed)
- 15 applications per organization covering every stage: applied, screening,
  interview, offer, hired, rejected, withdrawn; the closed job only has finished ones
- A full stage history per application (`stage_transitions`, with actor and reason);
  `version` equals the number of transitions
- Completed interviews with scorecards, upcoming interviews without them, and in
  each organization one interviewer with two overlapping interviews stored in
  different UTC offsets
- Reviewer notes and resume file metadata (PDF or DOCX, under 10 MiB)

All names come from the generator's word lists, users use `example.org`,
applicants use `example.com` and phone numbers are in the 555-01xx range.
Applicant records have no age, gender, ethnicity, nationality or photo fields.

## Edge cases

Added by default (leave them out with `--no-edge-cases`) and listed in
`manifest.json` with the check that must reject each one:

| Kind | Rejected by |
|---|---|
| `oversized_resume` | resume upload size limit (10 MiB) |
| `unsupported_file_type` | resume upload content-type allow-list |
| `duplicate_application` | unique (job, applicant) constraint |
| `prompt_injection_note` | summary pipeline treats notes as data, never as instructions |
| `cross_tenant_note` | organization scoping on notes |

`validate()` reports every rule violation except the listed edge cases, and the
CLI refuses to write a set that has any.

## Output

One `<table>.json` per table plus `manifest.json` (`fixture_version`, seed,
reference date, counts per table, edge cases and upload limits). The same seed
and reference date always produce byte-identical files.
