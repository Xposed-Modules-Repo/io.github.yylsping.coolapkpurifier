"""Locate and validate standard DEX/CDEX images inside a raw memory region."""

from __future__ import annotations

import argparse
import mmap
import struct
from pathlib import Path


DEX_MAGICS = tuple(b"dex\n0" + bytes((minor, 0)) for minor in range(ord("3"), ord("5")))
CDEX_MAGIC = b"cdex001\x00"
DEX_HEADER_SIGNATURE = b"\x70\x00\x00\x00\x78\x56\x34\x12"


def u32(data: mmap.mmap, offset: int) -> int:
    return struct.unpack_from("<I", data, offset)[0]


def find_all(data: mmap.mmap, needle: bytes):
    start = 0
    while True:
        found = data.find(needle, start)
        if found < 0:
            return
        yield found
        start = found + 1


def validate_dex(data: mmap.mmap, base: int) -> tuple[bool, list[str], int]:
    remaining = len(data) - base
    reasons: list[str] = []
    if remaining < 0x70:
        return False, ["truncated header"], 0

    file_size = u32(data, base + 0x20)
    header_size = u32(data, base + 0x24)
    endian_tag = u32(data, base + 0x28)
    if header_size != 0x70:
        reasons.append(f"header_size={header_size:#x}")
    if endian_tag not in (0x12345678, 0x78563412):
        reasons.append(f"endian_tag={endian_tag:#x}")
    if not 0x70 <= file_size <= remaining:
        reasons.append(f"file_size={file_size:#x} remaining={remaining:#x}")

    # Validate every fixed-width ID table against the advertised image size.
    tables = (
        ("string_ids", 0x38, 0x3C, 4),
        ("type_ids", 0x40, 0x44, 4),
        ("proto_ids", 0x48, 0x4C, 12),
        ("field_ids", 0x50, 0x54, 8),
        ("method_ids", 0x58, 0x5C, 8),
        ("class_defs", 0x60, 0x64, 32),
    )
    if file_size >= 0x70 and file_size <= remaining:
        for name, size_off, table_off, width in tables:
            count = u32(data, base + size_off)
            offset = u32(data, base + table_off)
            if count and (offset < 0x70 or offset + count * width > file_size):
                reasons.append(f"{name}=({count:#x},{offset:#x}) out-of-range")

        data_size = u32(data, base + 0x68)
        data_off = u32(data, base + 0x6C)
        if data_size and (data_off < 0x70 or data_off + data_size > file_size):
            reasons.append(f"data=({data_size:#x},{data_off:#x}) out-of-range")

    return not reasons, reasons, file_size


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("region", type=Path)
    parser.add_argument("--output-dir", type=Path)
    parser.add_argument("--carve", action="store_true")
    args = parser.parse_args()

    output_dir = args.output_dir or args.region.parent / "heap_carved"
    if args.carve:
        output_dir.mkdir(parents=True, exist_ok=True)

    with args.region.open("rb") as source:
        with mmap.mmap(source.fileno(), 0, access=mmap.ACCESS_READ) as data:
            candidate_map = {
                offset: (magic, "magic")
                for magic in (*DEX_MAGICS, CDEX_MAGIC)
                for offset in find_all(data, magic)
            }
            # Hardened runtimes often wipe only the magic/checksum/signature. The
            # adjacent header_size/endian_tag pair is a strong recovery anchor.
            for signature_offset in find_all(data, DEX_HEADER_SIGNATURE):
                base = signature_offset - 0x24
                if base >= 0:
                    candidate_map.setdefault(base, (data[base : base + 8], "header"))
            candidates = sorted(
                (offset, magic, source)
                for offset, (magic, source) in candidate_map.items()
            )
            print(f"region={args.region} size={len(data)} candidates={len(candidates)}")
            valid_count = 0
            for offset, magic, source in candidates:
                kind = "cdex" if magic == CDEX_MAGIC else "dex"
                if kind == "dex":
                    valid, reasons, file_size = validate_dex(data, offset)
                else:
                    # CompactDex uses a different header layout. Preserve candidates
                    # for a dedicated parser instead of applying standard DEX rules.
                    valid, reasons, file_size = False, ["compact-dex candidate"], 0
                status = "VALID" if valid else "candidate"
                detail = "; ".join(reasons) if reasons else "header/tables in-range"
                print(
                    f"{status} kind={kind} source={source} offset={offset:#x} "
                    f"file_size={file_size:#x} magic={magic!r} {detail}"
                )
                if valid:
                    valid_count += 1
                    if args.carve:
                        target = output_dir / f"heap_{offset:08x}_{file_size:x}.dex"
                        target.write_bytes(data[offset : offset + file_size])
                        print(f"  carved={target}")
            print(f"valid_standard_dex={valid_count}")


if __name__ == "__main__":
    main()
