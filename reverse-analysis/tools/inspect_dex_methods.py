"""Inspect exact classes/methods in a recovered DEX with Androguard."""

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
    parser.add_argument("--class-name", action="append", default=[])
    parser.add_argument("--method-name", action="append", default=[])
    parser.add_argument("--contains", action="append", default=[])
    parser.add_argument("--limit", type=int, default=50)
    parser.add_argument("--disassemble", action="store_true")
    args = parser.parse_args()

    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="backslashreplace")

    dex = DEX(args.dex.read_bytes())
    class_names = tuple(value.lower() for value in args.class_name)
    method_names = tuple(value.lower() for value in args.method_name)
    contains = tuple(value.lower() for value in args.contains)
    matches = []
    for cls in dex.get_classes():
        class_name = cls.get_name().lower()
        if class_names and not any(value in class_name for value in class_names):
            continue
        for method in cls.get_methods():
            method_name = method.get_name().lower()
            if method_names and method_name not in method_names:
                continue
            code = method.get_code()
            instructions = [] if code is None else list(code.get_bc().get_instructions())
            method_label = label(method)
            body = "\n".join(item.get_output() for item in instructions)
            if contains and not any(value in (method_label + "\n" + body).lower()
                                    for value in contains):
                continue
            matches.append((method, instructions))

    print(f"matches={len(matches)}")
    for method, instructions in matches[:args.limit]:
        print(f"\n=== {ascii(label(method))} ===")
        if args.disassemble:
            offset = 0
            for instruction in instructions:
                print(f"{offset:04x}: {instruction.get_name():<24} {instruction.get_output()}")
                offset += instruction.get_length()


if __name__ == "__main__":
    main()
