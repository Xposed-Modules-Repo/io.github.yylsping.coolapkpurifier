import ida_nalt

for i in range(ida_nalt.get_import_module_qty()):
    mod = ida_nalt.get_import_module_name(i)
    lst = []
    def make_cb(lst):
        def cb(ea, name=None, ordinal=None):
            try:
                if isinstance(name, bytes):
                    name = name.decode("latin-1")
            except Exception:
                name = repr(name)
            lst.append((hex(ea), name, ordinal))
            return True
        return cb
    try:
        ida_nalt.enum_import_names(i, make_cb(lst))
    except Exception as e:
        lst.append(("ERR", str(e), None))
    print("MODULE", mod, len(lst))
    for ea, name, ordinal in lst:
        try:
            print("  ", ea, name, ordinal)
        except Exception:
            print("  ", ea, repr(name), ordinal)
