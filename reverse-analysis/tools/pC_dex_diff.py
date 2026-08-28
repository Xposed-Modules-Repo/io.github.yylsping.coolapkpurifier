#!/usr/bin/env python3
"""Priority C: full string-pool diff of wrapper classes.dex 16.5.1 vs 16.6.1,
plus quick asset/entry diff of both APKs."""
import zipfile, re, hashlib, sys, io

BASE = r".\history-apks"
APKS = {
    "16.5.1": BASE + r"\cool-apk-16-5-1.apk",
    "16.6.1": BASE + r"\CoolApk-16.6.1-2608212-coolapk-arm64-sign.apk",
}

STR_RE = re.compile(rb"[\x20-\x7e]{6,}")

def dex_strings(path):
    with zipfile.ZipFile(path) as z:
        data = z.read("classes.dex")
    out = set()
    for m in STR_RE.finditer(data):
        s = m.group().decode("ascii", "replace")
        out.add(s)
    return out, hashlib.sha256(data).hexdigest()

def apk_entries(path):
    with zipfile.ZipFile(path) as z:
        return {i.filename: (i.file_size, i.CRC) for i in z.infolist()}

def main():
    s51, h51 = dex_strings(APKS["16.5.1"])
    s61, h61 = dex_strings(APKS["16.6.1"])
    print(f"16.5.1 dex sha256={h51} strings={len(s51)}")
    print(f"16.6.1 dex sha256={h61} strings={len(s61)}")
    added = s61 - s51
    removed = s51 - s61
    print(f"\nadded in 16.6.1: {len(added)}")
    print(f"removed in 16.6.1: {len(removed)}")

    # Focus on security/remote-config related
    KEYS = ["DDI", "PostToken", "nuid", "X-App", "shuzilm", "HTProtect", "aebd", "ioctl",
            "LSPosed", "Xposed", "Zygisk", "magisk", "root", "risk", "Risk", "coolapk_purifier",
            "installedApk", "useDDI", "MainInit", "checkLogin", "indexV8", "init", "security",
            "Security", "black", "Black", "warn", "Warn", "notice", "Notice", "announce"]
    print("\n--- ADDED (security/config relevant) ---")
    for s in sorted(added):
        if any(k in s for k in KEYS):
            print("+", s[:160])
    print("\n--- REMOVED (security/config relevant) ---")
    for s in sorted(removed):
        if any(k in s for k in KEYS):
            print("-", s[:160])

    e51 = apk_entries(APKS["16.5.1"])
    e61 = apk_entries(APKS["16.6.1"])
    added_e = set(e61) - set(e51)
    removed_e = set(e51) - set(e61)
    changed_e = {k for k in set(e51) & set(e61) if e51[k] != e61[k]}
    print(f"\nentries: 16.5.1={len(e51)} 16.6.1={len(e61)} added={len(added_e)} removed={len(removed_e)} changed={len(changed_e)}")
    for k in sorted(added_e)[:40]:
        print("+E", k)
    for k in sorted(removed_e)[:40]:
        print("-E", k)
    # libs changed
    print("\n--- changed native libs ---")
    for k in sorted(changed_e):
        if k.startswith("lib/") or k.endswith(".so"):
            print("*E", k, e51[k], "->", e61[k])

if __name__ == "__main__":
    main()
