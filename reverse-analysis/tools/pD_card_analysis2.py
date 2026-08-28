#!/usr/bin/env python3
"""Priority D v2: find classes by source-file name in both DEXes."""
import sys, logging
logging.disable(logging.DEBUG)
from androguard.core.dex import DEX

BIZ = r".tmp_audit/regions_clean/coolapk_business_restored.dex"
NET = r".tmp_audit/p3stream/main_useDDI.dex"

def scan(path, wanted):
    dv = DEX(open(path, "rb").read())
    found = {}
    for c in dv.get_classes():
        sf = dv.get_cm_string(c.get_source_file_idx()) or ""
        nm = c.get_name()
        for w in wanted:
            if w in sf or w in nm:
                found.setdefault(w, []).append((nm, sf))
    return found

WANTED = ["AlertMessageCardViewHolder", "MessageCardDialogFragment", "FeedDialogInterceptor",
          "FeedBlockSpamInterceptor", "FragmentFeedBlockInterceptor", "FeedWarning",
          "ItemAlertMessageCardBinding", "DialogMessageCardBinding", "PrivateMessageCardUtils",
          "FeedReplyRecommendDialogInterceptor", "FeedReplyTopDialogInterceptor"]

for path, tag in [(BIZ, "BIZ"), (NET, "NET")]:
    try:
        res = scan(path, WANTED)
        print(f"===== {tag} =====")
        for w, lst in res.items():
            print(f"-- {w}: {len(lst)}")
            for nm, sf in lst[:8]:
                print("   ", nm, "|", sf)
    except Exception as e:
        print(tag, "ERR", e)
