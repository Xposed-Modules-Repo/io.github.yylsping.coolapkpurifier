"""Minimal DEX parser: no map_list needed. Parses header id tables and
class_data to locate a method and summarize its code (invokes, field
accesses, const strings)."""
import struct
import sys


class Dex:
    def __init__(self, path):
        self.data = open(path, 'rb').read()
        d = self.data
        # header fields (offsets are carved-file offsets; magic at 0)
        self.file_size = u32(d, 0x20)
        self.string_ids_size = u32(d, 0x38)
        self.string_ids_off = u32(d, 0x3c)
        self.type_ids_size = u32(d, 0x40)
        self.type_ids_off = u32(d, 0x44)
        self.proto_ids_size = u32(d, 0x48)
        self.proto_ids_off = u32(d, 0x4c)
        self.field_ids_size = u32(d, 0x50)
        self.field_ids_off = u32(d, 0x54)
        self.method_ids_size = u32(d, 0x58)
        self.method_ids_off = u32(d, 0x5c)
        self.class_defs_size = u32(d, 0x60)
        self.class_defs_off = u32(d, 0x64)
        self._strings = {}

    # ---------- primitives ----------
    @staticmethod
    def read_uleb128(data, off):
        result = 0
        shift = 0
        while True:
            b = data[off]
            off += 1
            result |= (b & 0x7F) << shift
            if b < 0x80:
                break
            shift += 7
        return result, off

    def string(self, idx):
        if idx in self._strings:
            return self._strings[idx]
        off = u32(self.data, self.string_ids_off + idx * 4)
        _, pos = self.read_uleb128(self.data, off)  # utf16 length
        end = self.data.index(b'\x00', pos)
        s = self.data[pos:end].decode('utf-8', 'replace')
        self._strings[idx] = s
        return s

    def type_name(self, idx):
        return self.string(u32(self.data, self.type_ids_off + idx * 4))

    def proto(self, idx):
        off = self.proto_ids_off + idx * 12
        shorty = self.string(u32(self.data, off))
        ret = self.type_name(u32(self.data, off + 4))
        params_off = u32(self.data, off + 8)
        params = []
        if params_off:
            size = u32(self.data, params_off)
            for i in range(size):
                params.append(self.type_name(u16(self.data, params_off + 4 + i * 2)))
        return ret, params

    def method(self, idx):
        off = self.method_ids_off + idx * 8
        cls = self.type_name(u16(self.data, off))
        proto_idx = u16(self.data, off + 2)
        name = self.string(u32(self.data, off + 4))
        return cls, name, proto_idx

    def field(self, idx):
        off = self.field_ids_off + idx * 8
        cls = self.type_name(u16(self.data, off))
        ftype = self.type_name(u16(self.data, off + 2))
        name = self.string(u32(self.data, off + 4))
        return f"{cls}.{name}:{ftype}"

    # ---------- class defs ----------
    def find_class(self, descriptor_prefix):
        for i in range(self.class_defs_size):
            off = self.class_defs_off + i * 32
            try:
                type_idx = u32(self.data, off)
                name = self.type_name(type_idx)
            except (struct.error, IndexError):
                continue  # wiped entry; keep scanning
            if name.startswith(descriptor_prefix):
                return name, off
        return None, None

    def class_data(self, class_def_off):
        d = self.data
        class_data_off = u32(d, class_def_off + 24)
        if class_data_off == 0:
            return []
        pos = class_data_off
        static_fields, pos = self.read_uleb128(d, pos)
        instance_fields, pos = self.read_uleb128(d, pos)
        direct_methods, pos = self.read_uleb128(d, pos)
        virtual_methods, pos = self.read_uleb128(d, pos)
        for _ in range(static_fields + instance_fields):
            _, pos = self.read_uleb128(d, pos)  # field_idx_diff
            _, pos = self.read_uleb128(d, pos)  # access_flags
        methods = []
        for kind, count in (('direct', direct_methods), ('virtual', virtual_methods)):
            method_idx = 0
            for _ in range(count):
                diff, pos = self.read_uleb128(d, pos)
                access, pos = self.read_uleb128(d, pos)
                code_off, pos = self.read_uleb128(d, pos)
                method_idx += diff
                methods.append((method_idx, access, code_off, kind))
        return methods

    def code_item(self, code_off):
        d = self.data
        registers = u16(d, code_off)
        ins = u16(d, code_off + 2)
        outs = u16(d, code_off + 4)
        tries = u16(d, code_off + 6)
        debug_off = u32(d, code_off + 8)
        insns_size = u32(d, code_off + 12)
        insns_off = code_off + 16
        insns = d[insns_off:insns_off + insns_size * 2]
        return dict(registers=registers, ins=ins, outs=outs, tries=tries,
                    insns=insns, insns_off=insns_off)


def u16(data, off):
    return struct.unpack_from('<H', data, off)[0]


def u32(data, off):
    return struct.unpack_from('<I', data, off)[0]


def s16(v):
    return v - 0x10000 if v >= 0x8000 else v


# dalvik opcode families relevant for side-effect summary
INVOKE = {
    0x6e: 'invoke-virtual', 0x6f: 'invoke-super', 0x70: 'invoke-direct',
    0x71: 'invoke-static', 0x72: 'invoke-interface',
    0x74: 'invoke-virtual/range', 0x75: 'invoke-super/range',
    0x76: 'invoke-direct/range', 0x77: 'invoke-static/range',
    0x78: 'invoke-interface/range',
}
FIELDOPS = {
    0x52: 'iget', 0x53: 'iget-wide', 0x54: 'iget-object', 0x55: 'iget-bool',
    0x56: 'iget-byte', 0x57: 'iget-char', 0x58: 'iget-short',
    0x59: 'iput', 0x5a: 'iput-wide', 0x5b: 'iput-object', 0x5c: 'iput-bool',
    0x5d: 'iput-byte', 0x5e: 'iput-char', 0x5f: 'iput-short',
    0x60: 'sget', 0x61: 'sget-wide', 0x62: 'sget-object', 0x63: 'sget-bool',
    0x64: 'sget-byte', 0x65: 'sget-char', 0x66: 'sget-short',
    0x67: 'sput', 0x68: 'sput-wide', 0x69: 'sput-object', 0x6a: 'sput-bool',
    0x6b: 'sput-byte', 0x6c: 'sput-char', 0x6d: 'sput-short',
}


def summarize_code(dex, code):
    invokes = []
    fields = []
    strings = []
    insns = code['insns']
    i = 0
    n = len(insns) // 2
    while i < n:
        op = insns[i * 2]
        if op in INVOKE:
            idx = (insns[i * 2 + 1] << 8) | insns[i * 2 + 2]
            cls, name, proto_idx = dex.method(idx)
            invokes.append((INVOKE[op], f"{cls}->{name}"))
            i += 3
            continue
        if op in FIELDOPS:
            idx = (insns[i * 2 + 2] << 8) | insns[i * 2 + 3]
            fields.append((FIELDOPS[op], dex.field(idx)))
            i += 2
            continue
        if op == 0x1a:  # const-string
            idx = (insns[i * 2 + 1] << 8) | insns[i * 2 + 2]
            strings.append(dex.string(idx))
            i += 2
            continue
        if op == 0x1b:  # const-string/jumbo
            idx = u32(insns, i * 2 + 2)
            strings.append(dex.string(idx))
            i += 3
            continue
        # conservative skip: sizes per opcode format family
        i += 1
    return invokes, fields, strings


def main():
    path = sys.argv[1]
    class_query = sys.argv[2] if len(sys.argv) > 2 else 'Lkc5'
    dex = Dex(path)
    print(f"strings={dex.string_ids_size} types={dex.type_ids_size} "
          f"methods={dex.method_ids_size} classes={dex.class_defs_size}")
    name, cdef = dex.find_class(class_query)
    print("class:", name)
    if cdef is None:
        return
    for method_idx, access, code_off, kind in dex.class_data(cdef):
        cls, mname, proto_idx = dex.method(method_idx)
        ret, params = dex.proto(proto_idx)
        acc = []
        if access & 0x8: acc.append('static')
        if access & 0x10: acc.append('final')
        if access & 0x2: acc.append('private')
        print(f"\n=== {' '.join(acc)} {ret} {mname}({', '.join(params)}) "
              f"[{kind}] code@{code_off:#x}")
        if code_off == 0:
            print("  (abstract/native)")
            continue
        code = dex.code_item(code_off)
        print(f"  registers={code['registers']} ins={code['ins']} "
              f"insns={len(code['insns'])//2} tries={code['tries']}")
        invokes, fields, strings = summarize_code(dex, code)
        for kindop, target in invokes:
            print(f"  CALL {kindop} {target}")
        for kindop, target in fields:
            print(f"  FIELD {kindop} {target}")
        for s in strings:
            print(f"  STR {s!r}")


if __name__ == '__main__':
    main()
