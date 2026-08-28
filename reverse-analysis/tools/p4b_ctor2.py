#!/usr/bin/env python3
"""Struct-init extractor with pointer-offset tracking (write-back aware).
Self-test against 16.6.1 known ctor defaults, then extract 15.9.0's ctor."""
import sys, re
sys.path.insert(0, r".\.tmp_audit")
from p4b_align_scan import aligned_scan
from capstone.arm64 import ARM64_OP_IMM

OLD = open(r".\.tmp_audit\p4b_extract\15.9.0_arm64-v8a_libNetHTProtect.so","rb").read()
NEW = open(r".\.tmp_audit\p4b_extract\16.6.1_arm64-v8a_libNetHTProtect.so","rb").read()

def norm(r):
    r = r.strip()
    if r.startswith("w") and r[1:].isdigit():
        return "x" + r[1:]
    return r

def extract(data, start, end):
    regs = {}      # value regs
    ptr = {}       # reg -> offset from base (x0 at entry)
    stores = []    # (eff_off, width, value_or_None)
    ZERO = object()
    def setptr(r, off):
        ptr[r] = off
        regs.pop(r, None)
    # x0 is the struct base
    setptr("x0", 0)
    for insn in aligned_scan(data, start, end):
        m, ops = insn.mnemonic, insn.op_str
        s = ops.replace(" ", "")
        try:
            if m in ("mov", "movz") and ",#" in s and "[" not in s:
                r, v = s.split(",#")
                r = norm(r)
                try:
                    regs[r] = int(v, 0) & 0xFFFFFFFFFFFFFFFF
                    ptr.pop(r, None)
                except ValueError:
                    regs.pop(r, None); ptr.pop(r, None)
            elif m == "mov" and s.count(",") == 1 and "[" not in s:
                # reg-to-reg: propagate pointer offsets (and values)
                d, b = norm(s.split(",")[0]), norm(s.split(",")[1])
                if b in ptr:
                    setptr(d, ptr[b])
                elif b in regs:
                    regs[d] = regs[b]
                    ptr.pop(d, None)
                else:
                    regs.pop(d, None); ptr.pop(d, None)
            elif m == "movi":
                # v0.2d, #0 -> zero vector; track only reg name v0
                r = s.split(",")[0]
                regs[r] = ZERO
            elif m == "movk":
                parts = s.split(",")
                r = norm(parts[0])
                lsl = 0
                mm = re.search(r"lsl#?(\d+)", parts[-1])
                if mm:
                    lsl = int(mm.group(1))
                try:
                    regs[r] = (regs.get(r, 0) if isinstance(regs.get(r, 0), int) else 0) | (int(parts[1].replace("#",""), 0) << lsl)
                except (ValueError, TypeError):
                    pass
            elif m in ("mov",) and s.count(",")==1 and s.split(",")[1] in ("xzr",):
                regs[norm(s.split(",")[0])] = 0
                ptr.pop(norm(s.split(",")[0]), None)
            elif m == "add" and s.count(",") == 2 and s.split(",")[2].startswith("#"):
                d, b, imm = s.split(",")
                d, b = norm(d), norm(b)
                try:
                    off = int(imm.replace("#",""), 0)
                except ValueError:
                    continue
                if b in ptr:
                    setptr(d, ptr[b] + off)
                elif b == "sp":
                    pass
            elif m in ("str", "strb", "strh", "stur", "stp", "sturb", "sturh"):
                mm = re.search(r"\[(\w+)(?:,#?(-?(?:0x[0-9a-f]+|\d+)))?\](!)?$", s.split(",", 1)[1] if "," in s else s)
                if not mm:
                    # stp x, y, [base, #off]
                    mm = re.search(r"\[(\w+)(?:,#?(-?(?:0x[0-9a-f]+|\d+)))?\](!)?$", s)
                if not mm:
                    continue
                base = mm.group(1)
                off = int(mm.group(2), 0) if mm.group(2) else 0
                wb = mm.group(3) == "!"
                if base in ("sp", "x29"):
                    continue
                if base not in ptr:
                    continue
                eff = ptr[base] + off
                srcs = s.split("[")[0].split(",")
                if m == "stp":
                    # two regs, 8 bytes each (x) or 4 (w)
                    vals = []
                    for src in srcs:
                        src = norm(src)
                        vals.append(regs.get(src))
                    w = 8 if srcs[0].startswith("x") else 4
                    for i, v in enumerate(vals):
                        stores.append((eff + i*w, w, (v if isinstance(v, int) else (0 if v is ZERO else None))))
                elif m == "str":
                    src = norm(srcs[0])
                    v = regs.get(src)
                    w = 8 if srcs[0].startswith("x") else 4
                    stores.append((eff, w, (v if isinstance(v, int) else (0 if v is ZERO else None))))
                elif m in ("strb", "sturb"):
                    src = norm(srcs[0])
                    v = regs.get(src)
                    stores.append((eff, 1, (v if isinstance(v, int) else (0 if v is ZERO else None))))
                elif m in ("strh", "sturh"):
                    src = norm(srcs[0])
                    v = regs.get(src)
                    stores.append((eff, 2, (v if isinstance(v, int) else (0 if v is ZERO else None))))
                # NEON stur d0/q0/stp q — value 0
                elif srcs[0].startswith(("d", "q", "v", "s")):
                    sz = {"strb":1,"strh":2}.get(m, 8)
                    stores.append((eff, sz, 0 if regs.get(srcs[0]) is ZERO else None))
                if wb:
                    setptr(base, ptr[base] + off)
            elif m == "sub" and s.count(",") == 2 and s.split(",")[2].startswith("#"):
                mm2 = re.match(r"(\w+),(\w+),#(\w+)", s)
                if not mm2:
                    continue
                d, b, imm = mm2.group(1), mm2.group(2), mm2.group(3)
                d, b = norm(d), norm(b)
                if b in ptr:
                    try:
                        setptr(d, ptr[b] - int(imm, 0))
                    except ValueError:
                        pass
        except Exception:
            continue
    return stores

def fmt(stores):
    out = {}
    for off, w, v in stores:
        if v is None:
            continue
        out[off] = (w, v)
    return out

if __name__ == "__main__":
    print("== 16.6.1 ctor 0x252F68 ==")
    st = fmt(extract(NEW, 0x252F68, 0x252F68 + 0xd4c))
    for off in sorted(st):
        w, v = st[off]
        print(f"  +{off:#6x} w={w} v={v:#x}" + (f" ({v})" if v < 0x10000 else ""))
