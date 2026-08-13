-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# Hook handles and the coordinator are deliberate strong roots. Modern module entry
# instances are not guaranteed to remain strongly reachable after lifecycle callbacks.
-keep,allowobfuscation class io.github.coolapk.adblock.** {
    *;
}
