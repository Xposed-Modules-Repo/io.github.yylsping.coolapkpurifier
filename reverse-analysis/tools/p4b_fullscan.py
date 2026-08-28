#!/usr/bin/env python3
"""Full-text stack-string capability scan over libNetHTProtect .text.
Sliding windows -> reconstruct -> decode variants -> keyword grep."""
import sys, json, re
sys.path.insert(0, r".\.tmp_audit")
from stackstr import reconstruct, decode_variants

KEYWORDS = [
    # fragment-tolerant: match mid-word substrings since buffers may be split
    # across decode keys / stack slots (e.g. "ersioncode -U")
    "m list pack", "ckages -f", "ersioncode", "zygis", "roc/self/maps", "s-top-ap",
    "ase.ap", "at/arm6", "base.ode", "nline-max", "ibandroid_runtim",
    "hared_Clea", "eferenced", "roc/self/fdin", "om.androi", "nt_id",
    "ndroid/dat", "torage/e", "sposed", "posed", "agis", "rida", "ptrac",
    "filePermis", "installedA", "S_BF#A", "hangedPack", "id_match",
    "hizuku", "CCESSIBIL", "readclo",
]

def scan(data, start, end, win=0x800, step=0x400, kw_list=KEYWORDS):
    found = {}  # keyword -> [(addr, decoded)]
    for pos in range(start, end - win, step):
        runs = reconstruct(data, pos, pos + win, min_run=6)
        for off, raw in runs:
            variants = [("", "", raw.decode(errors="replace"))]
            variants += decode_variants(raw)
            for op, k, s in variants:
                for kw in kw_list:
                    if kw.lower() in s.lower():
                        found.setdefault(kw, []).append((hex(pos + off), op, k, s[:70]))
    return found

if __name__ == "__main__":
    which = sys.argv[1] if len(sys.argv) > 1 else "new"
    path = (r".\.tmp_audit\p4b_extract\15.9.0_arm64-v8a_libNetHTProtect.so"
            if which == "old" else
            r".\.tmp_audit\p4b_extract\16.6.1_arm64-v8a_libNetHTProtect.so")
    end = 0x44d22c if which == "old" else 0x456d40
    data = open(path, "rb").read()
    res = scan(data, 0x1000, end)
    # dedupe
    for kw, hits in sorted(res.items()):
        seen = set()
        uniq = []
        for h in hits:
            key = (h[1], h[2], h[3])
            if key not in seen:
                seen.add(key)
                uniq.append(h)
        print(f"== {kw} ({len(uniq)} uniq) ==")
        for addr, op, k, s in uniq[:4]:
            print(f"   {addr} {op:8s} k={k:5s} {s!r}")
    json.dump({k: v[:8] for k, v in res.items()},
              open(rf".\.tmp_audit\p4b_scan_{which}.json", "w"), indent=1)
