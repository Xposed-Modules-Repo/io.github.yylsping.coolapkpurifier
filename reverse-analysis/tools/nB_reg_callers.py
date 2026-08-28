#!/usr/bin/env python3
"""Scan all callers of sub_9B83C (key->offset mapper) and resolve the key constant
passed in W1 at each call site. Identify producers vs known readers for key 203695656."""
import json, idc, idautils

READER = 0x9B83C
KNOWN_READERS_203695656 = {
    0x28700,0x299FC,0x2A658,0x37784,0x39A80,0x3A8B8,0x3B2EC,0x3C030,0x3CCF0,
    0x3D644,0x3E9FC,0x3ED10,0x415CC,0x41E28,0x462B0,0x473F8,0x47ED8,0x49410,0x4A5EC,
}

def get_caller_func(ea):
    f = idc.get_func_attr(ea, idc.FUNCATTR_START)
    return f

sites = []
for x in idautils.CodeRefsTo(READER, 0):
    caller = get_caller_func(x)
    # walk back up to 40 instructions looking for MOV W1, #imm
    key = None
    ea = x
    for i in range(40):
        ea = idc.prev_head(ea)
        if ea == idc.BADADDR or ea < 0x1000:
            break
        mnem = idc.print_insn_mnem(ea)
        disasm = idc.GetDisasm(ea)
        if mnem == "MOV" and ("W1," in disasm or "W1," in disasm.upper()):
            # parse immediate
            if "#" in disasm:
                try:
                    val = disasm.split("#",1)[1].split(",")[0].strip()
                    key = int(val, 0)
                    break
                except Exception:
                    pass
        # stop at function start
        if ea == caller:
            break
    sites.append({"call": hex(x), "func": hex(caller) if caller != idc.BADADDR else None,
                  "fname": idc.get_func_name(caller) if caller != idc.BADADDR else None,
                  "key": key})

by_key = {}
unknown_203695656 = []
for s in sites:
    by_key.setdefault(s["key"], []).append(s)
    if s["key"] == 203695656:
        f = int(s["func"], 16) if s["func"] else None
        if f is not None and f not in KNOWN_READERS_203695656:
            unknown_203695656.append(s)

print(json.dumps({
    "total_sites": len(sites),
    "key_histogram": {str(k): len(v) for k, v in sorted(by_key.items(), key=lambda kv: -len(kv[1]))[:15]},
    "sites_203695656": [s for s in sites if s["key"] == 203695656],
    "unknown_funcs_203695656": unknown_203695656,
}, indent=1))
