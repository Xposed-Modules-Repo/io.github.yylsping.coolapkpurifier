"""Disassemble a virtual-address range from an AArch64 ELF."""

from __future__ import annotations

import argparse
from pathlib import Path

from capstone import CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN, Cs
from elftools.elf.elffile import ELFFile


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("elf", type=Path)
    parser.add_argument("start", type=lambda value: int(value, 0))
    parser.add_argument("end", type=lambda value: int(value, 0))
    args = parser.parse_args()
    with args.elf.open("rb") as stream:
        elf = ELFFile(stream)
        text = elf.get_section_by_name(".text")
        base = text["sh_addr"]
        offset = args.start - base
        code = text.data()[offset: offset + (args.end - args.start)]
    engine = Cs(CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN)
    for instruction in engine.disasm(code, args.start):
        print(f"{instruction.address:08x}: {instruction.mnemonic:<8} {instruction.op_str}")


if __name__ == "__main__":
    main()
