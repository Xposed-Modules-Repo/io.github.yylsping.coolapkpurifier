#!/usr/bin/env python3
"""Priority D v3: dump target obfuscated classes with method string constants."""
import sys, logging
logging.disable(logging.DEBUG)
from androguard.core.dex import DEX

BIZ = r".tmp_audit/regions_clean/coolapk_business_restored.dex"
TARGETS = ["Lк;", "Leha;", "Lcom/coolapk/market/view/message/MessageCardDialogFragment;"]

dv = DEX(open(BIZ, "rb").read())
by_name = {c.get_name(): c for c in dv.get_classes()}

def method_strings(m):
    try:
        code = m.get_code()
    except Exception:
        return []
    if code is None:
        return []
    out = []
    try:
        for ins in code.get_bc().get_instructions():
            if "string" in ins.get_name():
                o = ins.get_output()
                if '"' in o:
                    try:
                        out.append(o.split('"')[1])
                    except Exception:
                        pass
    except Exception:
        pass
    return out

for t in TARGETS:
    c = by_name.get(t)
    if not c:
        # try inner-class style search
        cands = [c2 for n, c2 in by_name.items() if n.startswith(t[:-1])]
        print(f"== {t}: not found directly, {len(cands)} inner candidates")
        for c2 in cands[:2]:
            t = c2.get_name()
            c = c2
            break
    if not c:
        continue
    print("=" * 70)
    print("CLASS", c.get_name(), "super:", c.get_superclassname())
    try:
        print("interfaces:", c.get_interfaces())
    except Exception:
        pass
    for m in c.get_methods():
        ms = method_strings(m)
        print(f"  M {m.get_name()} {m.get_descriptor()}")
        if ms:
            for s in ms[:15]:
                print("      str:", s[:120])
