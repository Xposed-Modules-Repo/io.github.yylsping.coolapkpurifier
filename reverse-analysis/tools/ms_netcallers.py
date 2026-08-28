#!/usr/bin/env python3
"""Find callers of network-related imports in metasec."""
import idc, idautils, json

res = {}
for nm in [".socket", ".connect", ".sendto", ".recvfrom", ".inet_addr", ".epoll_wait",
           ".popen", ".fork", ".dlopen", ".dlsym", ".syscall"]:
    ea = idc.get_name_ea_simple(nm)
    if ea == idc.BADADDR:
        res[nm] = "missing"
        continue
    xs = []
    for x in idautils.CodeRefsTo(ea, 0):
        xs.append({"from": hex(x), "fn": idc.get_func_name(x)})
    res[nm] = {"ea": hex(ea), "callers": xs}
print(json.dumps(res))
