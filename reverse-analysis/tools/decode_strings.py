"""Decode the obfuscated strings found in libNetHTProtect.so cache/append path."""

results = []


def dec_sub(text, delta):
    results.append(("SUB %d" % delta, text, "".join(chr((ord(c) - delta) & 0xFF) for c in text)))


def dec_xor(text, key):
    results.append(("XOR %d" % key, text, "".join(chr(ord(c) ^ key) for c in text)))


def dec_xor_idx(text, key):
    out = []
    for i, c in enumerate(text):
        out.append(chr((ord(c) ^ ((i + key) & 0xFF)) & 0xFF))
    results.append(("XOR(i+%d)" % key, text, "".join(out)))


def dec_bytes_sub(bs, delta):
    results.append(("SUB %d" % delta, repr(bs), "".join(chr((b - delta) & 0xFF) for b in bs)))


def dec_bytes_xor(bs, key):
    results.append(("XOR %d" % key, repr(bs), "".join(chr(b ^ key) for b in bs)))


def dec_bytes_xor_idx(bs, key):
    results.append(("XOR(i+%d)" % key, repr(bs), "".join(chr((b ^ ((i + key) & 0xFF)) & 0xFF) for i, b in enumerate(bs))))


def qword_le(value, n):
    return [(value >> (8 * i)) & 0xFF for i in range(n)]


# ---- sub_2FA14C strings ----
dec_sub("p}", 9)                                                # "gt"?
dec_sub("KKl~E", 11)                                            # rec+200 annotation separator
# marker: key=58, bytes 30,105,101,120,124,25,0x7B XOR 58
dec_bytes_xor([30, 105, 101, 120, 124, 25, 0x7B], 58)           # marker
# "@@": key=30, bytes 0x5E,0x5E XOR 30
dec_bytes_xor([0x5E, 0x5E], 30)                                 # framing
dec_sub("|pkfth{jo", 7)                                         # uid_match
dec_sub("CClqmhfw", 3)                                          # annotation +98
dec_bytes_sub([75, 75, 0x82, 125, 116, 127, 0x70, 0x7E, 0x70, 0x6E], 11)  # annotation +97/+99
dec_sub("AAcjoebdd", 1)                                         # annotation +100
dec_bytes_xor_idx([42, 43, 25, 3, 5, 1, 31, 6, 28], 106)        # annotation +101
dec_sub("BBujk|wmw", 2)                                         # annotation +103
dec_bytes_xor([1, 1] + [ord(c) for c in ",.\"*-.\""], 65)        # annotation +104
dec_bytes_xor_idx([57, 58, 8, 9, 93, 31, 17, 0xE4, 0xA1, 0xE3, 0xEF, 0xE1]
                  + qword_le(0xFEABEEE7E9A7F2F7, 8)
                  + [0xE8, 0xEF, 0xEB, 0xF3, 0xFD, 0xFB, 0xE3], 121)   # annotation +96/97/102
dec_sub("lqjwpnmYjltjpn|", 9)                                   # changedPackages
dec_sub("~~", 2)                                                # "||"
dec_sub("johunlkWhjrhnlz", 7)                                   # changedPackages key2
dec_sub("tmh>", 4)                                              # pid:
# /storage/emulated/0/Android/data (key 103)
dec_bytes_xor_idx([72, 27, 29, 5, 25, 13, 10, 11, 64, 21, 28, 7]
                  + qword_le(0x554957131301151F, 8)
                  + [58, 18, 25, 12, 16, 0xE9, 0xE5, 0xAD, 0xE7, 0xE5, 0xF1, 0xE7], 103)
dec_bytes_xor([0x23, 0, 0, 0, 0x06, 0x50, 0x0C, 0x06, 0x50, 0x00][4:9], 0x23)  # "%s/%s"
dec_bytes_xor([0x48, 0, 0, 0, 0x6D, 0x3B, 0x6D, 0x67, 0x3B][4:9], 0x48)        # "%s%s/%s"
dec_bytes_xor_idx([0x46, 0x45, 0x5E], 43)                        # "mis" event key
dec_sub("wtx", 4)                                               # event key in 29DC18

# ---- sub_2777D4 / sub_277EB0 keys ----
dec_bytes_xor([0x5A, 0x46], 38)                                 # "|a"?? actually key1 of label extract
dec_bytes_xor([0x30 + 0x48], 0x48)                              # placeholder no-op
dec_sub("zo|", 10)                                              # "per"
dec_sub("vhu", 3)                                               # "ser"
dec_bytes_xor([0x50, 0x45, 0x52], 32)                           # "per" (service loop)
dec_sub("sfr", 5)                                               # service field 2
dec_bytes_xor([0x19, 0x01, 0x0C, 0x0D] if False else [0x63, 0x68, 0x61, 0x6E], 0)  # skip

# sub_277EB0 int keys (template offsets -> record offsets)
dec_bytes_xor_idx([0x22, 0x36, 0x26, 0x26], 82)                 # a3+192 key
dec_bytes_xor([4, 6, 17, 22], 101)                              # a3+176 key
dec_bytes_xor([0x6D, 0x7B, 0x6C, 0x6D], 30)                     # a3+180 key
dec_sub("|om}", 10)                                             # a3+188 key
dec_sub("rtqu", 2)                                              # a3+184 key
dec_bytes_xor([0x59, 0x51, 0x40, 0x47], 52)                     # a3+196 key
dec_bytes_xor_idx([0x6D, 0x69, 0x6E], 0x74)                     # "min" (a3+208)
dec_bytes_xor_idx([0x74, 0x61, 0x72], 0x59)                     # "tar" (a3+232)
dec_bytes_xor([0x53, 0x5E, 0x61], 1)                            # a3+88 key1
dec_bytes_xor([0x6E, 0x70], 10)                                 # a3+112 key

# permission strings (sub_277EB0, template+72..80 flags)
dec_sub("ivlzwql6xmzuq{{qwv6IKKM[[g[]XMZ][MZ", 8)               # flag@t+72
dec_bytes_xor_idx([85, 91, 82, 69, 87, 80, 94, 21, 76, 88, 76, 82]
                  + qword_le(0x14682B2B2A313229, 8)
                  + [17, 26, 30, 14, 1, 18, 15, 3, 21, 3, 6, 12, 3, 28, 24, 19]
                  + [0x17, 0x0E], 52)                            # flag@t+73
dec_bytes_xor_idx([65, 79, 70, 81, 75, 76, 66, 9, 88, 76, 88, 70]
                  + qword_le(0x7A1C5F5F465D5E45, 8)
                  + [122, 127] + [ord(c) for c in "stlf"] + [127] + [ord(c) for c in "mysjl"], 32)  # flag@t+74
dec_xor("DKAWJLA\vU@WHLVVLJK\vrwlq`zv`fpw`zv`qqlkbv", 37)        # flag@t+75
dec_bytes_xor_idx([ord(c) for c in ", +\">;7z%3%50)(520q\"(,';$%$-:9\".$\"&$(- 1' >;<"], 77)  # flag@t+76
dec_xor("uzpf{}p:dqfy}gg}{z:AZ_Z[CZ", 20)                        # flag@t+77
dec_bytes_xor_idx([1, 14, 4, 18, 15, 9, 4, 78, 16, 5, 18, 13]
                  + qword_le(0x324E0E0F09131309, 8)
                  + [ord(c) for c in "%!$?#,)0\"/!2$"], 96)      # flag@t+78
dec_bytes_xor_idx([65, 66, 75, 1, 67, 89, 91, 73, 65, 94, 67, 25]
                  + qword_le(0x114C585B5A545855, 8)
                  + [ord(c) for c in "0$0.-65.''"] + [0x64, 0x0A, 12, 28, 4], 44)  # flag@t+79
dec_sub("ivlzwql6xmzuq{{qwv6ITTW_gUWKSgTWKI\\QWV[", 8)           # flag@t+80

# sub_273B74 parser markers
dec_sub("yjltjpnC", 9)                                          # package:
dec_sub("2etoA", 4)                                             # .apk=
dec_bytes_xor([ord(c) for c in "o|kjpvwZv}|#"], 25)              # versionCode:
dec_bytes_xor_idx([4, 24, 21, 0x4B], 113)                        # uid:

# misc
dec_sub("8y{xl8|nuo8xxvh|lx{nhjms", 9)                           # /proc/self/oom_score_adj
dec_sub("7xzwk7", 8)                                            # /proc/
dec_sub("qtlhfy%2h", 5)                                         # logcat -c
dec_sub("enqug\"hckngf#", 2)                                    # perror msg

for mode, enc, plain in results:
    print("%-12s %-30r -> %s" % (mode, enc[:30], plain))
