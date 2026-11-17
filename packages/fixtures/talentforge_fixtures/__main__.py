"""Command line entry point: python -m talentforge_fixtures --out <dir>."""

import argparse
import sys
from datetime import date

from .generator import DEFAULT_REFERENCE_DATE, DEFAULT_SEED, generate, validate, write_fixture_set


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description="Generate synthetic TalentForge hiring data.")
    parser.add_argument("--out", required=True, help="output directory")
    parser.add_argument("--seed", type=int, default=DEFAULT_SEED)
    parser.add_argument("--reference-date", type=date.fromisoformat, default=DEFAULT_REFERENCE_DATE)
    parser.add_argument("--no-edge-cases", action="store_true",
                        help="leave out the deliberate problem records")
    args = parser.parse_args(argv)

    data = generate(args.seed, args.reference_date, edge_cases=not args.no_edge_cases)
    problems = validate(data)
    if problems:
        print("\n".join(problems), file=sys.stderr)
        return 1
    paths = write_fixture_set(data, args.out)
    manifest = data["manifest"]
    print(f"Wrote {len(paths)} files to {args.out} (fixture_version {manifest['fixture_version']}, "
          f"{len(manifest['edge_cases'])} edge cases)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
