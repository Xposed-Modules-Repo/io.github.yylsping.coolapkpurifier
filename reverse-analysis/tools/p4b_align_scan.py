#!/usr/bin/env python3
"""Aligned linear AArch64 decoder: yields (addr, mnemonic, imm-operands), keeps 4-byte alignment."""
from capstone import Cs, CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN
from capstone.arm64 import ARM64_OP_IMM

md = Cs(CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN)
md.detail = True

def aligned_scan(data, start, end):
    pos = start
    while pos < end:
        found = False
        for insn in md.disasm(data[pos:end], pos):
            found = True
            yield insn
            pos = insn.address + insn.size
        if not found:
            pos += 4

def imm_stream(data, start, end):
    for insn in aligned_scan(data, start, end):
        for op in insn.operands:
            if op.type == ARM64_OP_IMM:
                yield insn.address, insn.mnemonic, op.imm & 0xFFFFFFFF
