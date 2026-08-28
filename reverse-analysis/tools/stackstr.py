#!/usr/bin/env python3
"""Stack-string reconstructor for AArch64: tracks mov/movz/movk -> strb/strh/str [sp,#off]
within a window and rebuilds the constructed buffers. Then applies decode hints."""
import sys
sys.path.insert(0, r".\.tmp_audit")
from p4b_align_scan import aligned_scan

def _norm_reg(r):
    r = r.strip()
    if r.startswith("w") and r[1:].isdigit():
        return "x" + r[1:]
    return r

def reconstruct(data, start, end, min_run=5):
    regs = {}
    ptrs = {}   # reg -> sp-relative offset (add xN, sp, #imm)
    stack = {}
    insns = list(aligned_scan(data, start, end))
    def store_bytes(off, b):
        for i, x in enumerate(b):
            o = off + i
            if o >= 0:  # negative sp offsets can't belong to a constructed buffer here
                stack[o] = x
    def base_off(base_reg, op_off):
        """effective sp offset for a store base register; None if not sp-derived"""
        if base_reg in ("sp", "x29"):
            return op_off
        return (ptrs.get(base_reg) + op_off) if base_reg in ptrs else None
    for idx, insn in enumerate(insns):
        m, ops = insn.mnemonic, insn.op_str
        try:
            if m in ("mov", "movz") and ", #" in ops:
                r, v = ops.split(", #")
                r = _norm_reg(r)
                try:
                    regs[r] = int(v.split()[0], 0) & 0xFFFFFFFFFFFFFFFF
                    ptrs.pop(r, None)
                except ValueError:
                    regs.pop(r, None); ptrs.pop(r, None)
            elif m == "mov" and ops.split(",")[-1].strip() == "sp":
                r = _norm_reg(ops.split(",")[0])
                ptrs[r] = 0; regs.pop(r, None)
            elif m == "add" and "#0" not in ops.split("sp")[-1][:3]:
                parts = [p.strip() for p in ops.split(",")]
                if len(parts) == 3 and parts[2].startswith("#"):
                    dst = _norm_reg(parts[0])
                    try:
                        imm = int(parts[2].replace("#","").replace("0x","0x"), 0)
                    except ValueError:
                        continue
                    if parts[1] == "sp":
                        ptrs[dst] = imm; regs.pop(dst, None)
                    elif _norm_reg(parts[1]) in ptrs:
                        ptrs[dst] = ptrs[_norm_reg(parts[1])] + imm; regs.pop(dst, None)
            elif m == "movk" and ", #" in ops:
                parts = [p.strip() for p in ops.split(",")]
                r, v = _norm_reg(parts[0]), parts[1]
                lsl = 0
                if len(parts) >= 3 and "lsl" in parts[2]:
                    lsl = int(parts[2].split("#")[1])
                try:
                    val = int(v, 0) << lsl
                    regs[r] = (regs.get(r, 0) | val) & 0xFFFFFFFFFFFFFFFF
                except ValueError:
                    pass
            elif m in ("strb", "strh", "str", "stur") and "[" in ops and ops.split("[")[0].count(",") == 1:
                r = _norm_reg(ops.split(",")[0])
                br_off = parse_sp_off(ops)
                if br_off is None:
                    # base may be a pointer reg like [x8, #0x18]
                    import re as _re
                    mm = _re.search(r"\[(\w+)(?:,#?(-?(?:0x[0-9a-f]+|\d+)))?\]", ops.replace(" ", ""))
                    if not mm:
                        continue
                    base = mm.group(1)
                    op_off = int(mm.group(2), 0) if mm.group(2) else 0
                    off = base_off(base, op_off)
                else:
                    import re as _re
                    mm = _re.search(r"\[(\w+)", ops.replace(" ", ""))
                    base = mm.group(1) if mm else "sp"
                    off = base_off(base, br_off)
                if off is None or r not in regs:
                    continue
                if m == "strb":
                    store_bytes(off, bytes([regs[r] & 0xFF]))
                elif m == "strh":
                    store_bytes(off, (regs[r] & 0xFFFF).to_bytes(2, "little"))
                else:
                    w = 4 if ops.split(",")[0].strip().startswith("w") else 8
                    store_bytes(off, (regs[r] & ((1 << (w*8)) - 1)).to_bytes(w, "little"))
        except Exception:
            continue
    # extract printable runs
    out = []
    if not stack:
        return out
    stack = {o: v for o, v in stack.items() if o >= 0}
    if not stack:
        return out
    maxoff = max(stack)
    buf = bytearray(maxoff + 2)
    for o, v in stack.items():
        buf[o] = v
    cur = bytearray()
    cur_off = 0
    for o in range(maxoff + 1):
        b = buf[o]
        if 0x20 <= b < 0x7f:
            if not cur:
                cur_off = o
            cur.append(b)
        else:
            if len(cur) >= min_run:
                out.append((cur_off, bytes(cur)))
            cur = bytearray()
    if len(cur) >= min_run:
        out.append((cur_off, bytes(cur)))
    return out

def parse_sp_off(ops):
    import re
    s = ops.replace(" ", "")
    m = re.search(r"\[(?:sp|x29)(?:,#?(-?(?:0x[0-9a-f]+|\d+)))?\]", s)
    if not m:
        return None
    if m.group(1) is None:
        return 0
    try:
        return int(m.group(1), 0)
    except ValueError:
        return None

def decode_variants(b):
    outs = []
    for k in range(1, 256):
        d = bytes(((x - k) & 0xFF) for x in b)
        if all(0x20 <= c < 0x7f or c == 0 for c in d):
            outs.append(("sub", hex(k), d.decode(errors="replace")))
        d = bytes((x ^ k) for x in b)
        if all(0x20 <= c < 0x7f or c == 0 for c in d):
            outs.append(("xor", hex(k), d.decode(errors="replace")))
    for k in range(0, 256):
        d = bytes((x ^ ((i + k) & 0xFF)) & 0xFF for i, x in enumerate(b))
        if all(0x20 <= c < 0x7f or c == 0 for c in d):
            outs.append(("rollxor", hex(k), d.decode(errors="replace")))
        d = bytes(((x - i - k) & 0xFF) for i, x in enumerate(b))
        if all(0x20 <= c < 0x7f or c == 0 for c in d):
            outs.append(("rollsub", hex(k), d.decode(errors="replace")))
    return outs

if __name__ == "__main__":
    NEW = open(r".\.tmp_audit\p4b_extract\16.6.1_arm64-v8a_libNetHTProtect.so","rb").read()
    # validate: run_pm_list (0x26FCEC, 0x2f4) should build the pm command
    print("== validate 16.6.1 sub_26FCEC ==")
    for off, b in reconstruct(NEW, 0x26FCEC, 0x26FCEC + 0x2f4):
        print(f"  [{off:#x}] raw={b!r}")
        for op, k, s in decode_variants(b)[:4]:
            print(f"        {op} k={k} -> {s!r}")
