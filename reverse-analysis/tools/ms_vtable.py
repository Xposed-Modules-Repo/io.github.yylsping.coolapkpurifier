#!/usr/bin/env python3
"""Dump qwords around 0x1E6138 and find code that reads that region (data refs via offset patterns)."""
import idc, idautils, json, ida_bytes

base = 0x1E60A0
out = []
for ea in range(base, base + 0x100, 8):
    v = ida_bytes.get_qword(ea)
    name = idc.get_func_name(v)
    nm = idc.get_name(ea)
    out.append({"ea": hex(ea), "name": nm, "val": hex(v), "func": name})

# find xrefs to the whole window
xrefs = {}
for ea in range(base, base + 0x100, 8):
    for x in idautils.XrefsTo(ea):
        xrefs.setdefault(hex(ea), []).append({"from": hex(x.frm), "fn": idc.get_func_name(x.frm), "type": x.type})

print(json.dumps({"window": out, "xrefs": xrefs}, ensure_ascii=False))
