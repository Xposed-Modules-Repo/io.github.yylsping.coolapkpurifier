"""Trim a memory-dumped DEX and recompute its signature/checksum header."""

from __future__ import annotations

import argparse
import hashlib
import struct
import zlib
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    data = bytearray(args.source.read_bytes())
    if data[:4] != b"dex\n":
        raise SystemExit(f"not a DEX image: {data[:8]!r}")
    file_size = struct.unpack_from("<I", data, 0x20)[0]
    data = data[:file_size]
    data[12:32] = hashlib.sha1(data[32:]).digest()
    struct.pack_into("<I", data, 8, zlib.adler32(data[12:]) & 0xFFFFFFFF)
    args.output.write_bytes(data)
    print(f"output={args.output} size={len(data)}")


if __name__ == "__main__":
    main()
