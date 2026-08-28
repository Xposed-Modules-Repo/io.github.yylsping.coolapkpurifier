#!/usr/bin/env python3
"""Recover the REAL .dynsym/.dynstr of libnesec.so via program headers + PT_DYNAMIC,
bypassing the deliberately corrupted section table."""
import struct, sys

PATH = r".tmp_audit/native/libnesec.so"
data = open(PATH, "rb").read()

e_phoff, = struct.unpack_from("<Q", data, 0x20)
e_phentsize, e_phnum = struct.unpack_from("<HH", data, 0x36)
print(f"phoff={e_phoff:#x} phentsize={e_phentsize} phnum={e_phnum}")

PT = {1: "LOAD", 2: "DYNAMIC", 6: "PHDR", 7: "TLS", 0x6474e550: "GNU_EH_FRAME", 0x6474e551: "GNU_STACK", 0x6474e552: "GNU_RELRO"}
loads = []      # (vaddr, filesz, memsz, offset, flags)
dynamic = None
for i in range(e_phnum):
    off = e_phoff + i * e_phentsize
    p_type, p_flags, p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_align = struct.unpack_from("<IIQQQQQQ", data, off)
    print(f"  {PT.get(p_type, hex(p_type)):<14} off={p_offset:#x} vaddr={p_vaddr:#x} filesz={p_filesz:#x} memsz={p_memsz:#x} flags={p_flags:#x}")
    if p_type == 1:
        loads.append((p_vaddr, p_filesz, p_memsz, p_offset, p_flags))
    if p_type == 2:
        dynamic = (p_offset, p_vaddr, p_filesz)

def vaddr_to_off(v):
    for vaddr, filesz, memsz, offset, flags in loads:
        if vaddr <= v < vaddr + filesz:
            return offset + (v - vaddr)
    return None

# parse .dynamic
dyn = {}
off = dynamic[0]
end = off + dynamic[2]
DT = {0: "NULL", 1: "NEEDED", 5: "STRTAB", 6: "SYMTAB", 10: "STRSZ", 11: "SYMENT", 0x6ffffef5: "GNU_HASH", 4: "HASH", 0x17: "REL", 0x47: "RELA", 0x6ffffff9: "RELACZ", 0x6ffffffa: "RELASZ", 7: "RELA", 24: "INIT_ARRAY", 25: "FINI_ARRAY", 26: "INIT_ARRAYSZ", 27: "FINI_ARRAYSZ", 0x1e: "FLAGS", 0x6ffffffb: "FLAGS_1", 3: "PLTGOT", 0x2: "PLTRELSZ", 0x14: "PLTREL", 23: "JMPREL", 0x6ffffffe: "VERNEED", 0x6fffffff: "VERNEEDNUM", 0x6ffffff0: "VERSYM", 0x6ffffffc: "RELCOUNT", 0x6ffffffd: "RELACOUNT"}
while off < end:
    d_tag, d_val = struct.unpack_from("<qQ", data, off)
    if d_tag == 0:
        break
    name = DT.get(d_tag, hex(d_tag))
    dyn[name] = d_val
    off += 16
print("\n.dynamic tags:")
for k, v in sorted(dyn.items(), key=lambda x: str(x[0])):
    print(f"  {k} = {v:#x}")

strtab_off = vaddr_to_off(dyn["STRTAB"])
symtab_off = vaddr_to_off(dyn["SYMTAB"])
syment = dyn.get("SYMENT", 24)
strsz = dyn.get("STRSZ", 0)
print(f"\nstrtab@{strtab_off:#x} size={strsz:#x}  symtab@{symtab_off:#x} syment={syment}")

# We don't know symbol count directly; derive from hash table (GNU_HASH or HASH)
def parse_sysv_hash(off):
    nbucket, nchain = struct.unpack_from("<II", data, off)
    return nchain

count = None
if "HASH" in dyn:
    count = parse_sysv_hash(vaddr_to_off(dyn["HASH"]))
    print("SysV hash nchain =", count)
elif "GNU_HASH" in dyn:
    gh = vaddr_to_off(dyn["GNU_HASH"])
    nbuckets, symoffset, bloom_size, bloom_shift = struct.unpack_from("<IIII", data, gh)
    buckets_off = gh + 16 + bloom_size * 8
    buckets = struct.unpack_from(f"<{nbuckets}I", data, buckets_off)
    last = max(buckets) if buckets else symoffset
    chain_off = buckets_off + nbuckets * 4
    # walk chain from last symbol until end marker
    idx = last
    while True:
        v, = struct.unpack_from("<I", data, chain_off + (idx - symoffset) * 4)
        idx += 1
        if v & 1:
            break
    count = idx
    print("GNU_HASH derived symbol count =", count)

def read_cstr(off):
    end2 = data.find(b"\x00", off)
    return data[off:end2].decode("latin-1", "replace")

print(f"\n--- REAL .dynsym ({count} entries) ---")
exports = []
imports = []
for i in range(count):
    so = symtab_off + i * syment
    st_name, st_info, st_other, st_shndx, st_value, st_size = struct.unpack_from("<IBBHQQ", data, so)
    if st_name == 0:
        continue
    nm = read_cstr(strtab_off + st_name)
    bind = st_info >> 4
    typ = st_info & 0xF
    if st_shndx == 0:  # SHN_UNDEF = import
        if nm:
            imports.append((nm, bind, typ))
    elif st_shndx != 0xfff1:
        exports.append((nm, hex(st_value), st_size, bind, typ))

print(f"\nIMPORTS ({len(imports)}):")
for nm, bind, typ in imports:
    print(f"  {nm}  bind={bind} type={typ}")
print(f"\nEXPORTS ({len(exports)}):")
for nm, val, sz, bind, typ in exports:
    print(f"  {nm} @ {val} size={sz} bind={bind} type={typ}")
