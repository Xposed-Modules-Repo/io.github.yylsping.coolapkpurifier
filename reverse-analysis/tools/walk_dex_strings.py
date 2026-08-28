"""Walk sorted DEX string_data_items forward, tolerating small inter-item gaps."""

from __future__ import annotations

import argparse
from collections import Counter
from pathlib import Path
import re

from recover_string_block import decode_mutf8, locate_item_start, parse_item, read_uleb128


TYPE_DESCRIPTOR = re.compile(r"^(?:[VZBSCIJFD]|\[*L[^\x00]+;|\[+[ZBSCIJFD])$")


def parse_item_with_slop(data: bytes, start: int, length_slop: int):
    declared, text_start = read_uleb128(data, start)
    end = data.index(0, text_start)
    text, actual_units = decode_mutf8(data[text_start:end])
    if abs(actual_units - declared) > length_slop:
        raise ValueError(
            f"utf16 length mismatch declared={declared} actual={actual_units}"
        )
    return end + 1, text, actual_units - declared


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("dex", type=Path)
    parser.add_argument("needle")
    parser.add_argument("--max-gap", type=int, default=64)
    parser.add_argument("--length-slop", type=int, default=2)
    parser.add_argument("--total-strings", type=int, default=0)
    parser.add_argument("--show-global-start", type=int, default=-1)
    parser.add_argument("--show-global-end", type=int, default=-1)
    parser.add_argument("--find-text", action="append", default=[])
    args = parser.parse_args()

    data = args.dex.read_bytes()
    value_offset = data.index(args.needle.encode())
    current_start = locate_item_start(data, value_offset)
    records: list[tuple[int, str]] = []
    gaps: Counter[int] = Counter()
    length_deltas: Counter[int] = Counter()

    while True:
        current_end, current_text, current_delta = parse_item_with_slop(
            data, current_start, args.length_slop
        )
        length_deltas[current_delta] += 1
        records.append((current_start, current_text))
        next_record = None
        for gap in range(args.max_gap + 1):
            candidate = current_end + gap
            try:
                _, text, _ = parse_item_with_slop(data, candidate, args.length_slop)
            except (UnicodeDecodeError, ValueError, IndexError):
                continue
            if text >= current_text:
                next_record = (candidate, text, gap)
                break
        if next_record is None:
            break
        current_start, _, gap = next_record
        gaps[gap] += 1

    print(
        f"start={records[0][0]:#x} first={ascii(records[0][1])} "
        f"end={current_end:#x} last={ascii(records[-1][1])} records={len(records)}"
    )
    print(f"gaps={dict(sorted(gaps.items()))}")
    print(f"length_deltas={dict(sorted(length_deltas.items()))}")
    if args.total_strings:
        anchor_global = args.total_strings - len(records)
        print(
            f"inferred_anchor_string_index={anchor_global} "
            f"of_total={args.total_strings}"
        )
        if args.show_global_start >= 0 and args.show_global_end >= args.show_global_start:
            selected = [
                (anchor_global + index, offset, text)
                for index, (offset, text) in enumerate(records)
                if args.show_global_start <= anchor_global + index <= args.show_global_end
            ]
            descriptors = sum(bool(TYPE_DESCRIPTOR.match(text)) for _, _, text in selected)
            print(
                f"global_range={args.show_global_start}..{args.show_global_end} "
                f"records={len(selected)} type_descriptors={descriptors}"
            )
            for global_index, offset, text in selected[:10] + selected[-10:]:
                print(
                    f"global={global_index:6d} offset={offset:#010x} "
                    f"type={bool(TYPE_DESCRIPTOR.match(text))} {ascii(text)}"
                )
        for query in args.find_text:
            matches = [
                (anchor_global + index, offset, text)
                for index, (offset, text) in enumerate(records)
                if query in text
            ]
            print(f"find_text={query!r} matches={len(matches)}")
            for global_index, offset, text in matches:
                print(f"  global={global_index} offset={offset:#x} {ascii(text)}")
    for index, (offset, text) in enumerate(records[-20:], len(records) - 20):
        print(f"{index:6d} {offset:#010x} {ascii(text)}")


if __name__ == "__main__":
    main()
