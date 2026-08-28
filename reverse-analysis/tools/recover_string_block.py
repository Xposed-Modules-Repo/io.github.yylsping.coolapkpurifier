"""Recover a contiguous DEX string_data_item block around a known ASCII value."""

from __future__ import annotations

import argparse
from pathlib import Path


def read_uleb128(data: bytes, offset: int) -> tuple[int, int]:
    value = 0
    shift = 0
    for _ in range(5):
        byte = data[offset]
        offset += 1
        value |= (byte & 0x7F) << shift
        if byte < 0x80:
            return value, offset
        shift += 7
    raise ValueError("invalid uleb128")


def decode_mutf8(raw: bytes) -> tuple[str, int]:
    units: list[int] = []
    offset = 0
    while offset < len(raw):
        first = raw[offset]
        if 0x01 <= first <= 0x7F:
            units.append(first)
            offset += 1
        elif 0xC0 <= first <= 0xDF and offset + 1 < len(raw):
            second = raw[offset + 1]
            if second & 0xC0 != 0x80:
                raise UnicodeDecodeError("mutf-8", raw, offset, offset + 2, "bad continuation")
            units.append(((first & 0x1F) << 6) | (second & 0x3F))
            offset += 2
        elif 0xE0 <= first <= 0xEF and offset + 2 < len(raw):
            second, third = raw[offset + 1], raw[offset + 2]
            if second & 0xC0 != 0x80 or third & 0xC0 != 0x80:
                raise UnicodeDecodeError("mutf-8", raw, offset, offset + 3, "bad continuation")
            units.append(
                ((first & 0x0F) << 12) | ((second & 0x3F) << 6) | (third & 0x3F)
            )
            offset += 3
        else:
            raise UnicodeDecodeError("mutf-8", raw, offset, offset + 1, "bad lead byte")
    packed = b"".join(unit.to_bytes(2, "little") for unit in units)
    return packed.decode("utf-16-le", "surrogatepass"), len(units)


def parse_item(data: bytes, start: int, expected_end: int | None = None):
    declared, text_start = read_uleb128(data, start)
    end = data.index(0, text_start)
    if expected_end is not None and end != expected_end:
        raise ValueError("unexpected terminator")
    raw = data[text_start:end]
    text, actual_units = decode_mutf8(raw)
    if actual_units != declared:
        raise ValueError(
            f"utf16 length mismatch declared={declared} actual={actual_units}"
        )
    return end + 1, text


def locate_item_start(data: bytes, value_offset: int) -> int:
    for start in range(max(0, value_offset - 5), value_offset):
        try:
            declared, text_start = read_uleb128(data, start)
        except (IndexError, ValueError):
            continue
        if text_start == value_offset:
            raw_end = data.index(0, value_offset)
            text, actual_units = decode_mutf8(data[value_offset:raw_end])
            if actual_units == declared:
                return start
    raise ValueError("could not locate string_data_item start")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("dex", type=Path)
    parser.add_argument("needle")
    args = parser.parse_args()

    data = args.dex.read_bytes()
    needle = args.needle.encode()
    value_offset = data.index(needle)
    anchor = locate_item_start(data, value_offset)

    starts = [anchor]
    current = anchor
    while current > 0:
        previous_end = current - 1
        if data[previous_end] != 0:
            break
        previous_separator = data.rfind(b"\x00", 0, previous_end)
        if previous_separator < 0:
            break
        previous_start = previous_separator + 1
        try:
            _, text = parse_item(data, previous_start, previous_end)
        except (UnicodeDecodeError, ValueError, IndexError):
            break
        starts.append(previous_start)
        current = previous_start
    starts.reverse()

    current, _ = parse_item(data, anchor)
    while current < len(data):
        try:
            next_offset, _ = parse_item(data, current)
        except (UnicodeDecodeError, ValueError, IndexError):
            break
        starts.append(current)
        current = next_offset

    records = []
    for start in starts:
        _, text = parse_item(data, start)
        records.append((start, text))
    anchor_index = starts.index(anchor)
    monotonic_pairs = sum(
        records[i - 1][1] <= records[i][1] for i in range(1, len(records))
    )
    print(
        f"anchor_value_offset={value_offset:#x} anchor_item_offset={anchor:#x} "
        f"block_start={starts[0]:#x} block_end={current:#x} records={len(records)} "
        f"anchor_index={anchor_index} monotonic_pairs={monotonic_pairs}/{max(0, len(records)-1)}"
    )
    print(f"first={ascii(records[0][1])} last={ascii(records[-1][1])}")
    for index in range(max(0, anchor_index - 10), min(len(records), anchor_index + 11)):
        offset, text = records[index]
        escaped = ascii(text)
        print(f"{index:6d} {offset:#010x} {escaped}")


if __name__ == "__main__":
    main()
