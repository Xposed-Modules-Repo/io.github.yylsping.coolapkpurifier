package io.github.yylsping.manifestgen;

import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Dumps the raw dex-level shape of a class for adaptation debugging. */
final class Probe {
    private Probe() {
    }

    static void run(java.util.Map<String, String> options) throws Exception {
        String input = options.get("--apk");
        String type = options.get("--class");
        String classFile = options.get("--class-file");
        if (classFile != null) {
            // Unicode-safe input path: descriptor read from a UTF-8 file so
            // obfuscated names survive shell/console code pages.
            type = new String(Files.readAllBytes(new File(classFile).toPath()),
                    StandardCharsets.UTF_8).trim();
        }
        String listPrefix = unicodeArg(options.get("--list-prefix"));
        String findSig = unicodeArg(options.get("--find-method"));
        String findLayout = unicodeArg(options.get("--find-clinit-layout"));
        String findFieldType = unicodeArg(options.get("--find-field-type"));
        String clinit = options.get("--clinit");
        String outPath = options.get("--out");
        if (input == null || (type == null && listPrefix == null && findSig == null
                && findLayout == null && findFieldType == null)) {
            System.out.println("usage: probe --apk <apk|dex|dir> "
                    + "[--class <Lcom/example/Foo;> | --class-file <utf8 file> "
                    + "| --list-prefix <Lcom/example/>; "
                    + "| --find-method <(params)ret[,static]>] [--clinit <Lowner;>] [--out <utf8 file>]");
            return;
        }
        DexIndex index = DexIndex.load(new File(input));
        StringBuilder out = new StringBuilder();
        if (findLayout != null) {
            String needle = "Lcom/coolapk/market/R$layout;->" + findLayout + ":I";
            for (ClassDef classDef : index.allClasses()) {
                for (Method method : classDef.getMethods()) {
                    if (!"<clinit>".equals(method.getName())) {
                        continue;
                    }
                    org.jf.dexlib2.iface.MethodImplementation impl = method.getImplementation();
                    if (impl == null) {
                        continue;
                    }
                    String pending = null;
                    for (org.jf.dexlib2.iface.instruction.Instruction insn : impl.getInstructions()) {
                        if (insn instanceof org.jf.dexlib2.iface.instruction.ReferenceInstruction) {
                            String ref = ((org.jf.dexlib2.iface.instruction.ReferenceInstruction) insn)
                                    .getReference().toString();
                            String op = insn.getOpcode().name;
                            if (op.startsWith("sget") && ref.equals(needle)) {
                                pending = ref;
                            } else if (op.startsWith("sput") && pending != null) {
                                out.append(classDef.getType()).append(" field=")
                                        .append(ref).append(" layout=").append(findLayout).append('\n');
                                pending = null;
                            }
                        }
                    }
                }
            }
        } else if (findFieldType != null) {
            for (ClassDef classDef : index.allClasses()) {
                for (Field field : classDef.getFields()) {
                    if (field.getType().equals(findFieldType)) {
                        out.append(classDef.getType()).append(" field=")
                                .append(field.getName()).append(':').append(field.getType())
                                .append(" access=0x")
                                .append(Integer.toHexString(field.getAccessFlags())).append('\n');
                    }
                }
            }
        } else if (findSig != null) {
            String sig = findSig;
            boolean wantStatic = sig.endsWith(",static");
            if (wantStatic) {
                sig = sig.substring(0, sig.length() - ",static".length());
            }
            for (ClassDef classDef : index.allClasses()) {
                for (Method method : classDef.getMethods()) {
                    if (wantStatic && !DexIndex.isStatic(method)) {
                        continue;
                    }
                    String d = DexIndex.methodDescriptor(method);
                    if (d.contains(sig)) {
                        out.append(d).append(" access=0x")
                                .append(Integer.toHexString(method.getAccessFlags())).append('\n');
                    }
                }
            }
        } else if (listPrefix != null) {
            for (ClassDef classDef : index.allClasses()) {
                if (classDef.getType().startsWith(listPrefix)) {
                    out.append(classDef.getType()).append('\n');
                }
            }
        } else {
            String descriptor = type.startsWith("L") ? type : Specs.classDescriptorOf(type);
            ClassDef classDef = index.classByDescriptor(descriptor);
            if (classDef == null) {
                out.append("class not found: ").append(descriptor)
                        .append(" (indexed ").append(index.classCount()).append(" classes)\n");
            } else {
                out.append("class ").append(classDef.getType())
                        .append(" access=0x").append(Integer.toHexString(classDef.getAccessFlags()))
                        .append('\n');
                out.append("superclass: ").append(classDef.getSuperclass()).append('\n');
                out.append("interfaces:");
                for (String iface : classDef.getInterfaces()) {
                    out.append(' ').append(iface);
                }
                out.append('\n');
                out.append("fields:\n");
                for (Field field : classDef.getFields()) {
                    out.append("  ").append(field.getName()).append(':').append(field.getType())
                            .append(" access=0x").append(Integer.toHexString(field.getAccessFlags()))
                            .append('\n');
                }
                out.append("methods:\n");
                for (Method method : classDef.getMethods()) {
                    out.append("  ").append(DexIndex.methodDescriptor(method))
                            .append(" access=0x").append(Integer.toHexString(method.getAccessFlags()))
                            .append('\n');
                }
                if (clinit != null) {
                    out.append("clinit-instructions:\n");
                    for (Method method : classDef.getMethods()) {
                        if (!"<clinit>".equals(method.getName())) {
                            continue;
                        }
                        org.jf.dexlib2.iface.MethodImplementation impl = method.getImplementation();
                        if (impl == null) {
                            continue;
                        }
                        for (org.jf.dexlib2.iface.instruction.Instruction insn : impl.getInstructions()) {
                            out.append("  ").append(insn.getOpcode().name);
                            if (insn instanceof org.jf.dexlib2.iface.instruction.ReferenceInstruction) {
                                out.append(' ').append(((org.jf.dexlib2.iface.instruction.ReferenceInstruction) insn)
                                        .getReference().toString());
                            }
                            if (insn instanceof org.jf.dexlib2.iface.instruction.WideLiteralInstruction) {
                                out.append(" #").append(((org.jf.dexlib2.iface.instruction.WideLiteralInstruction) insn)
                                        .getWideLiteral());
                            }
                            out.append('\n');
                        }
                    }
                }
            }
        }
        if (outPath != null) {
            Files.write(new File(outPath).toPath(),
                    out.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("wrote " + outPath);
        } else {
            System.out.print(out);
        }
    }

    private static String unicodeArg(String value) throws java.io.IOException {
        if (value != null && value.startsWith("@")) {
            return new String(Files.readAllBytes(new File(value.substring(1)).toPath()),
                    StandardCharsets.UTF_8).trim();
        }
        return value;
    }
}
