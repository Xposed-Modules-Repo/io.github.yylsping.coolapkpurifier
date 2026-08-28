"""Restore a DEX region that begins at magic[4:] after its first four bytes were wiped."""

from __future__ import annotations

import argparse
import hashlib
import struct
import zlib
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("region", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    source = args.region.read_bytes()
    if source[:4] not in (b"035\x00", b"036\x00", b"037\x00", b"038\x00", b"039\x00", b"040\x00", b"041\x00"):
        raise SystemExit(f"unexpected magic tail: {source[:8]!r}")
    file_size = struct.unpack_from("<I", source, 0x1C)[0]
    restored = b"dex\n" + source[: file_size - 4]
    if len(restored) != file_size:
        raise SystemExit(f"short region: restored={len(restored)} header={file_size}")

    expected_checksum = struct.unpack_from("<I", restored, 8)[0]
    actual_checksum = zlib.adler32(restored[12:]) & 0xFFFFFFFF
    expected_signature = restored[12:32]
    actual_signature = hashlib.sha1(restored[32:]).digest()
    args.output.write_bytes(restored)
    print(
        f"output={args.output} size={file_size} "
        f"adler32_expected={expected_checksum:08x} actual={actual_checksum:08x} "
        f"sha1_expected={expected_signature.hex()} actual={actual_signature.hex()} "
        f"valid={expected_checksum == actual_checksum and expected_signature == actual_signature}"
    )


if __name__ == "__main__":
    main()
