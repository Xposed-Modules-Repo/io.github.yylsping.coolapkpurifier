#!/usr/bin/env python3
"""Parse ELF .dynsym of libmetasec_ml.so: list exported (defined) symbols."""
import struct, sys

path = sys.argv[1] if len(sys.argv) > 1 else r".\.tmp_audit\native\libmetasec_ml.so"
data = open(path, "rb").read()

# ELF64 header
e_phoff = struct.unpack_from("<Q", data, 0x20)[0]
e_phentsize = struct.unpack_from("<H", data, 0x36)[0]
e_phnum = struct.unpack_from("<H", data, 0x38)[0]

dyn = None
segs = []
for i in range(e_phnum):
    off = e_phoff + i * e_phentsize
    p_type, p_flags = struct.unpack_from("<II", data, off)
    p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_align = struct.unpack_from("<QQQQQQ", data, off + 8)
    segs.append((p_type, p_offset, p_vaddr, p_filesz, p_memsz))
    if p_type == 2:  # PT_DYNAMIC
        dyn = (p_offset, p_filesz)

def vaddr_to_off(v):
    for t, o, va, fs, ms in segs:
        if t == 1 and va <= v < va + ms:
            return o + (v - va)
    return None

d = {}
off, size = dyn
for i in range(size // 16):
    tag, val = struct.unpack_from("<QQ", data, off + i * 16)
    if tag == 0:
        break
    d.setdefault(tag, []).append(val)

DT_STRTAB, DT_SYMTAB, DT_HASH, DT_GNU_HASH, DT_STRSZ = 5, 6, 4, 0x6ffffef5, 10
strtab_off = vaddr_to_off(d[DT_STRTAB][0])
symtab_v = d[DT_SYMTAB][0]

# symbol count: from gnu hash chain or symtab-strtab gap
sym_off = vaddr_to_off(symtab_v)
# use DT_GNU_HASH to find highest symbol index
def gnuhash_count(hash_v):
    h = vaddr_to_off(hash_v)
    nbuckets, symoffset, bloom_size, bloom_shift = struct.unpack_from("<IIII", data, h)
    buckets_off = h + 16 + bloom_size * 8
    buckets = struct.unpack_from("<%dI" % nbuckets, data, buckets_off)
    last = max(buckets) if buckets else 0
    if last < symoffset:
        return symoffset
    # walk chain
    chain_off = buckets_off + nbuckets * 4
    idx = last
    while True:
        val = struct.unpack_from("<I", data, chain_off + (idx - symoffset) * 4)[0]
        idx += 1
        if val & 1:
            break
    return idx

if DT_GNU_HASH in d:
    nsyms = gnuhash_count(d[DT_GNU_HASH][0])
elif DT_HASH in d:
    h = vaddr_to_off(d[DT_HASH][0])
    nsyms = struct.unpack_from("<I", data, h + 4)[0]
else:
    nsyms = (sym_off_gap := None) or 0

print("symbol count:", nsyms)
print("=== DEFINED symbols (st_value != 0) ===")
out = []
for i in range(nsyms):
    s = sym_off + i * 24
    st_name, st_info, st_other, st_shndx, st_value, st_size = struct.unpack_from("<IBBHQQ", data, s)
    if st_shndx == 0 or st_value == 0:
        continue
    name_off = strtab_off + st_name
    name = data[name_off:data.index(b"\x00", name_off)].decode("utf-8", "replace")
    typ = st_info & 0xF
    bind = st_info >> 4
    out.append((st_value, st_size, name, typ, bind, st_other))

out.sort()
for v, sz, n, t, b, o in out:
    kind = {1: "OBJ", 2: "FUNC", 0: "NOTYPE"}.get(t, str(t))
    extra = f" vis={o}" if o else ""
    print(f"0x{v:08x} size={sz:<6} {kind:6} bind={b} {n}{extra}")
