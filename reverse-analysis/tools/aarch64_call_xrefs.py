"""List direct AArch64 BL sites targeting an address in an ELF text section."""

from __future__ import annotations

import argparse
from pathlib import Path

from capstone import CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN, Cs
from capstone.arm64 import ARM64_OP_IMM
from elftools.elf.elffile import ELFFile


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("elf", type=Path)
    parser.add_argument("target", type=lambda value: int(value, 0))
    args = parser.parse_args()
    with args.elf.open("rb") as stream:
        elf = ELFFile(stream)
        text = elf.get_section_by_name(".text")
        code = text.data()
        address = text["sh_addr"]
    engine = Cs(CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN)
    engine.detail = True
    for instruction in engine.disasm(code, address):
        if instruction.mnemonic == "bl" and instruction.operands \
                and instruction.operands[0].type == ARM64_OP_IMM \
                and instruction.operands[0].imm == args.target:
            print(f"{instruction.address:#x}: {instruction.mnemonic} {instruction.op_str}")


if __name__ == "__main__":
    main()
