#!/usr/bin/env python3
"""Priority D v4: xref - find who instantiates/refs Lк; (AlertMessageCardViewHolder)
and dump the surrounding dispatch strings."""
import logging
logging.disable(logging.DEBUG)
from androguard.core.dex import DEX

BIZ = r".tmp_audit/regions_clean/coolapk_business_restored.dex"
TARGET = "Lк;"

dv = DEX(open(BIZ, "rb").read()
        )
cls_by_name = {c.get_name(): c for c in dv.get_classes()}
print("classes:", len(cls_by_name))

hits = []
for c in dv.get_classes():
    for m in c.get_methods():
        try:
            code = m.get_code()
        except Exception:
            code = None
        if code is None:
            continue
        try:
            for ins in code.get_bc().get_instructions():
                o = ins.get_output()
                if TARGET in o:
                    hits.append((c.get_name(), m.get_name(), m.get_descriptor(), ins.get_name(), o[:80]))
                    break
        except Exception:
            continue

print("xref hits:", len(hits))
for h in hits[:30]:
    print(" ", h)
