"""Locate simple ADR/ADRP+ADD references to a file-offset string in AArch64 ELF text."""

from __future__ import annotations

import argparse
from collections import deque
from pathlib import Path

from capstone import CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN, Cs
from capstone.arm64 import ARM64_OP_IMM, ARM64_OP_REG
from elftools.elf.elffile import ELFFile


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("elf", type=Path)
    parser.add_argument("target", type=lambda value: int(value, 0))
    parser.add_argument("--context", type=int, default=12)
    args = parser.parse_args()
    with args.elf.open("rb") as stream:
        elf = ELFFile(stream)
        text = elf.get_section_by_name(".text")
        code = text.data()
        address = text["sh_addr"]

    engine = Cs(CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN)
    engine.detail = True
    history: deque = deque(maxlen=12)
    pages: dict[int, tuple[int, int]] = {}
    instructions = list(engine.disasm(code, address))
    for index, instruction in enumerate(instructions):
        operands = instruction.operands
        if instruction.mnemonic == "adrp" and len(operands) >= 2 \
                and operands[0].type == ARM64_OP_REG \
                and operands[1].type == ARM64_OP_IMM:
            pages[operands[0].reg] = (operands[1].imm, index)
        elif instruction.mnemonic == "adr" and len(operands) >= 2 \
                and operands[1].type == ARM64_OP_IMM \
                and operands[1].imm == args.target:
            show(instructions, index, args.target, args.context)
        elif instruction.mnemonic == "add" and len(operands) >= 3 \
                and operands[0].type == ARM64_OP_REG \
                and operands[1].type == ARM64_OP_REG \
                and operands[2].type == ARM64_OP_IMM:
            base = pages.get(operands[1].reg)
            if base is not None and index - base[1] <= 16 \
                    and base[0] + operands[2].imm == args.target:
                show(instructions, index, args.target, args.context)
        history.append(instruction)


def show(instructions, index: int, target: int, context: int) -> None:
    print(f"\n=== xref target={target:#x} instruction={instructions[index].address:#x} ===")
    for instruction in instructions[max(0, index - context): index + context + 1]:
        marker = ">" if instruction.address == instructions[index].address else " "
        print(f"{marker} {instruction.address:08x}: {instruction.mnemonic:<8} {instruction.op_str}")


if __name__ == "__main__":
    main()
