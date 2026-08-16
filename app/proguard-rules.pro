-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# Hook handles and the coordinator are deliberate strong roots. Modern module entry
# instances are not guaranteed to remain strongly reachable after lifecycle callbacks.
-keep,allowobfuscation class io.github.yylsping.coolapkpurifier.** {
    *;
}

# DexKit is loaded from a dynamically extracted native library. Keep all of
# its descriptors because JNI registration and FlatBuffers query classes are
# referenced reflectively/natively.
-keep class org.luckypray.dexkit.** { *; }
-keep class com.google.flatbuffers.** { *; }
-dontwarn org.luckypray.dexkit.**
