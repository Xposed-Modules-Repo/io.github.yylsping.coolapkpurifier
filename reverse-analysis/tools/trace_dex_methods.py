"""Print restored-DEX methods whose bytecode references selected text."""

from __future__ import annotations

import argparse
from pathlib import Path
import sys

from loguru import logger

logger.remove()

from androguard.core.dex import DEX  # noqa: E402


def label(method) -> str:
    return f"{method.get_class_name()}->{method.get_name()}{method.get_descriptor()}"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("dex", type=Path)
    parser.add_argument("needle", nargs="+")
    parser.add_argument("--limit", type=int, default=100)
    parser.add_argument("--disassemble", action="store_true")
    parser.add_argument("--class-name", action="append", default=[])
    args = parser.parse_args()

    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="backslashreplace")

    dex = DEX(args.dex.read_bytes())
    needles = tuple(value.lower() for value in args.needle)
    matches = []
    for cls in dex.get_classes():
        for method in cls.get_methods():
            code = method.get_code()
            if code is None:
                continue
            instructions = list(code.get_bc().get_instructions())
            rendered = [instruction.get_output() for instruction in instructions]
            method_label = label(method).lower()
            body = "\n".join(rendered).lower()
            class_selected = any(
                value.lower() in method.get_class_name().lower()
                for value in args.class_name
            )
            if class_selected or any(needle in body or needle in method_label
                                     for needle in needles):
                matches.append((method, instructions))

    print(f"matches={len(matches)}")
    for method, instructions in matches[: args.limit]:
        print(f"\n=== {ascii(label(method))} ===")
        if args.disassemble:
            offset = 0
            for instruction in instructions:
                print(f"{offset:04x}: {instruction.get_name():<24} {instruction.get_output()}")
                offset += instruction.get_length()


if __name__ == "__main__":
    main()
