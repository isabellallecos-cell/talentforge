"""Acceptance tests for the fixture generator (docs/acceptance/week-01, AC-01.6 to AC-01.15)."""

import copy
import json
import tempfile
import unittest
from collections import defaultdict
from datetime import date, datetime
from pathlib import Path

from talentforge_fixtures import (
    EDGE_CASE_CHECKS, FIXTURE_VERSION, PROTECTED_FIELDS, ROLES, STAGES, TABLES,
    generate, validate, write_fixture_set,
)
from talentforge_fixtures.__main__ import main

TENANT_TABLES = [t for t in TABLES if t not in ("organizations", "users")]


def read_all(directory: str) -> dict:
    root = Path(directory)
    return {p.name: p.read_bytes() for p in sorted(root.iterdir())}


class GeneratorTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = generate()
        cls.edge_ids = {e["id"] for e in cls.data["manifest"]["edge_cases"]}
        cls.by_id = {r["id"]: r for t in TABLES for r in cls.data[t]}

    def records(self, table):
        return [r for r in self.data[table] if r["id"] not in self.edge_ids]

    # AC-01.6
    def test_same_seed_writes_identical_bytes(self):
        with tempfile.TemporaryDirectory() as a, tempfile.TemporaryDirectory() as b:
            write_fixture_set(generate(), a)
            write_fixture_set(generate(), b)
            self.assertEqual(read_all(a), read_all(b))

    # AC-01.7
    def test_different_seeds_differ_and_both_validate(self):
        other = generate(seed=7)
        self.assertNotEqual(json.dumps(self.data, sort_keys=True), json.dumps(other, sort_keys=True))
        for seed in (7, 99, 12345):
            self.assertEqual(validate(generate(seed=seed)), [], f"seed {seed}")
        self.assertEqual(validate(generate(reference_date=date(2027, 6, 30))), [])

    # AC-01.8
    def test_manifest_describes_the_set(self):
        manifest = self.data["manifest"]
        self.assertEqual(manifest["fixture_version"], FIXTURE_VERSION)
        self.assertEqual(manifest["seed"], 2207)
        self.assertEqual(manifest["reference_date"], "2026-02-02")
        self.assertEqual(manifest["counts"], {t: len(self.data[t]) for t in TABLES})
        self.assertEqual({e["kind"] for e in manifest["edge_cases"]}, set(EDGE_CASE_CHECKS))

    # AC-01.9
    def test_every_record_belongs_to_one_known_organization(self):
        orgs = {o["id"] for o in self.data["organizations"]}
        self.assertGreaterEqual(len(orgs), 2)
        for table in TENANT_TABLES:
            for record in self.data[table]:
                self.assertIn(record["organization_id"], orgs, record["id"])

    # AC-01.10
    def test_references_stay_inside_the_organization(self):
        members = {(m["organization_id"], m["user_id"]) for m in self.data["memberships"]}
        for app in self.records("applications"):
            self.assertEqual(self.by_id[app["job_id"]]["organization_id"], app["organization_id"])
        for table in ("interviews", "scorecards", "notes", "resume_files"):
            for record in self.records(table):
                app = self.by_id[record["application_id"]]
                self.assertEqual(app["organization_id"], record["organization_id"], record["id"])
        for interview in self.records("interviews"):
            self.assertIn((interview["organization_id"], interview["interviewer_id"]), members)
        cross = [n for n in self.data["notes"]
                 if self.by_id[n["application_id"]]["organization_id"] != n["organization_id"]]
        self.assertEqual([n["id"] for n in cross],
                         [e["id"] for e in self.data["manifest"]["edge_cases"]
                          if e["kind"] == "cross_tenant_note"])

    # AC-01.11
    def test_roles_job_states_and_stages_are_covered(self):
        roles = defaultdict(set)
        for m in self.data["memberships"]:
            roles[m["organization_id"]].add(m["role"])
        for org in self.data["organizations"]:
            self.assertEqual(roles[org["id"]], set(ROLES), org["id"])
        self.assertEqual({j["status"] for j in self.data["jobs"]}, {"open", "closed"})
        self.assertEqual({a["stage"] for a in self.records("applications")}, set(STAGES))
        closed = {j["id"] for j in self.data["jobs"] if j["status"] == "closed"}
        for app in self.data["applications"]:
            if app["job_id"] in closed:
                self.assertIn(app["stage"], {"hired", "rejected", "withdrawn"})

    # AC-01.12
    def test_stage_history_is_consistent(self):
        history = defaultdict(list)
        for step in self.data["stage_transitions"]:
            history[step["application_id"]].append(step)
        for app in self.data["applications"]:
            steps = sorted(history[app["id"]], key=lambda s: datetime.fromisoformat(s["occurred_at"]))
            self.assertEqual([s["sequence"] for s in steps], list(range(1, len(steps) + 1)), app["id"])
            previous = None
            for step in steps:
                self.assertEqual(step["from_stage"], previous, step["id"])
                self.assertTrue(step["actor_id"] and step["reason"], step["id"])
                previous = step["to_stage"]
            self.assertEqual(previous, app["stage"], app["id"])
            self.assertEqual(len(steps), app["version"], app["id"])

    # AC-01.13
    def test_overlapping_interviews_in_different_offsets(self):
        for record in self.data["interviews"]:
            for key in ("starts_at", "ends_at"):
                self.assertIsNotNone(datetime.fromisoformat(record[key]).utcoffset(), record["id"])
        found = []
        by_interviewer = defaultdict(list)
        for i in self.data["interviews"]:
            by_interviewer[(i["organization_id"], i["interviewer_id"])].append(i)
        for items in by_interviewer.values():
            for n, a in enumerate(items):
                for b in items[n + 1:]:
                    a_start, a_end = (datetime.fromisoformat(a[k]) for k in ("starts_at", "ends_at"))
                    b_start, b_end = (datetime.fromisoformat(b[k]) for k in ("starts_at", "ends_at"))
                    if a_start < b_end and b_start < a_end and a_start.utcoffset() != b_start.utcoffset():
                        found.append((a["id"], b["id"]))
        self.assertGreaterEqual(len(found), 1)

    # AC-01.14
    def test_people_are_plainly_fictional(self):
        for user in self.data["users"]:
            self.assertTrue(user["email"].endswith("@example.org"), user["id"])
        for applicant in self.data["applicants"]:
            self.assertTrue(applicant["email"].endswith("@example.com"), applicant["id"])
            self.assertRegex(applicant["phone"], r"^\+1-202-555-01\d\d$")
            self.assertFalse(PROTECTED_FIELDS & set(applicant), applicant["id"])
        for org in self.data["organizations"]:
            self.assertIn("(Synthetic)", org["name"])

    # AC-01.15
    def test_edge_cases_are_present_and_each_is_caught(self):
        listed = self.data["manifest"]["edge_cases"]
        for entry in listed:
            self.assertIn(entry["id"], {r["id"] for r in self.data[entry["table"]]})
            self.assertEqual(entry["rejected_by"], EDGE_CASE_CHECKS[entry["kind"]])
        self.assertEqual(validate(self.data), [])
        unlisted = copy.deepcopy(self.data)
        unlisted["manifest"]["edge_cases"] = []
        flagged = {p.split(":")[0] for p in validate(unlisted)}
        self.assertEqual(flagged, {e["id"] for e in listed})

    def test_edge_cases_can_be_left_out(self):
        clean = generate(edge_cases=False)
        self.assertEqual(clean["manifest"]["edge_cases"], [])
        self.assertEqual(validate(clean), [])

    def test_validate_reports_broken_references(self):
        broken = copy.deepcopy(self.data)
        broken["interviews"][0]["interviewer_id"] = "usr-9999"
        broken["applicants"][0]["gender"] = "x"
        problems = "\n".join(validate(broken))
        self.assertIn("interviewer_id is not a member", problems)
        self.assertIn("protected fields present", problems)

    def test_cli_writes_one_file_per_table_and_manifest(self):
        with tempfile.TemporaryDirectory() as out:
            self.assertEqual(main(["--out", out, "--seed", "11"]), 0)
            self.assertEqual(sorted(read_all(out)), sorted(f"{t}.json" for t in TABLES + ["manifest"]))
            manifest = json.loads((Path(out) / "manifest.json").read_text())
            self.assertEqual(manifest["seed"], 11)


if __name__ == "__main__":
    unittest.main()
