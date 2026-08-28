"""Extract printable ASCII strings from binary files for focused static triage."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("path", nargs="+", type=Path)
    parser.add_argument("--minimum", type=int, default=4)
    args = parser.parse_args()
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="backslashreplace")
    pattern = re.compile(rb"[\x20-\x7e]{%d,}" % args.minimum)
    for path in args.path:
        print(f"=== {path} ===")
        for match in pattern.finditer(path.read_bytes()):
            print(f"{match.start():08x} {match.group().decode('ascii')}")


if __name__ == "__main__":
    main()
