#!/usr/bin/env python3
"""Scan for JNIEnv vtable accesses: FindClass (idx 33, +0x108), NewStringUTF(167,+0x538),
GetStringUTFChars(169,+0x548), GetMethodID(33? no 33=FindClass; GetMethodID idx=33? no).
JNINativeInterface indices (AArch64, 8-byte pointers):
 GetJavaVM=6(+0x30)? actually GetJavaVM idx=219? no.
Standard indices: GetVersion=4, DefineClass=5, FindClass=6? NO.
Correct: idx: 0-3 reserved, GetVersion=4, DefineClass=5, FindClass=6, FromReflectedMethod=7...
Hmm! FindClass is index 6, not 33. Let me recompute: JNINativeInterface order:
 reserved0-3 (0..3), GetVersion (4), DefineClass (5), FindClass (6), FromReflectedMethod (7),
 FromReflectedField (8), ToReflectedMethod (9), GetSuperclass (10), IsAssignableFrom (11),
 ToReflectedField (12), Throw (13), ThrowNew (14), ExceptionOccurred (15), ExceptionDescribe (16),
 ExceptionClear (17), FatalError (18), PushLocalFrame (19), PopLocalFrame (20), NewGlobalRef (21),
 DeleteGlobalRef (22), DeleteLocalRef (23), IsSameObject (24), NewLocalRef (25), EnsureLocalCapacity (26),
 AllocObject (27), NewObject (28), NewObjectV (29), NewObjectA (30), GetObjectClass (31), IsInstanceOf (32),
 GetMethodID (33), Call<Type>Method... (34..), GetStaticMethodID (113)? ...
 Actually: object methods: CallObjectMethod=34, ... CallStaticObjectMethod? static call family starts 114?
 Standard: GetMethodID=33, CallObjectMethod=34, CallBooleanMethod=35, ... CallNonvirtual family 61..86?
 GetFieldID=94, GetObjectField=95..., GetStaticFieldID=144, GetStaticObjectField=145...,
 GetStaticMethodID=113, CallStaticObjectMethod=114, ...,
 GetStringLength=164, GetStringChars=165, ReleaseStringChars=166, NewStringUTF=167,
 GetStringUTFLength=168, GetStringUTFChars=169, ReleaseStringUTFChars=170,
 GetArrayLength=171..., RegisterNatives=215, UnregisterNatives=216, MonitorEnter=217, MonitorExit=218,
 GetJavaVM=219, GetStringRegion=220, GetStringUTFRegion=221, GetPrimitiveArrayCritical=222 ...
So offsets (x8): FindClass=6 -> 0x30; GetMethodID=33 -> 0x108; GetStaticMethodID=113 -> 0x388;
NewStringUTF=167 -> 0x538; GetStringUTFChars=169 -> 0x548; RegisterNatives=215 -> 0x6B8;
GetJavaVM=219 -> 0x6D8; ExceptionClear=17 -> 0x88; NewGlobalRef=21 -> 0xA8; DeleteLocalRef=23 -> 0xB8.
NOTE: in sub_6C3A4, v14[33] (0x108) = GetMethodID (not FindClass), v14[169] = GetStringUTFChars,
v14[113] = GetStaticMethodID, v14[215] = RegisterNatives, *(a1)+48 = v14[6] = FindClass.
So the 5 checked functions: FindClass, GetMethodID, GetStaticMethodID, GetStringUTFChars, RegisterNatives.
This scan: find all loads at these offsets.
"""
import idc, idautils, json

OFFS = {0x30: "FindClass(6)", 0x88: "ExceptionClear(17)", 0x108: "GetMethodID(33)",
        0x388: "GetStaticMethodID(113)", 0x538: "NewStringUTF(167)", 0x548: "GetStringUTFChars(169)",
        0x6B8: "RegisterNatives(215)", 0x6D8: "GetJavaVM(219)"}

hits = []
for fstart in idautils.Functions():
    fend = idc.find_func_end(fstart)
    if fend == idc.BADADDR:
        continue
    ea = fstart
    while ea < fend:
        if idc.print_insn_mnem(ea) == "LDR":
            d = idc.GetDisasm(ea)
            for off, nm in OFFS.items():
                tag = "#0x%X]" % off
                if tag in d or ("#0x%x]" % off) in d:
                    hits.append({"func": hex(fstart), "ea": hex(ea), "off": nm, "disasm": d})
                    break
        ea = idc.next_head(ea, fend)

summary = {}
for h in hits:
    summary.setdefault(h["off"], []).append(h["func"] + "@" + h["ea"])
print(json.dumps(summary, indent=1))
