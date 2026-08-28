package io.github.yylsping.coolapkpurifier;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import java.lang.reflect.Method;
import org.junit.Test;
import static org.junit.Assert.*;

public class SplashDiagnosticResolverTest {
    public static final class AdSource implements Parcelable {
        public AdSource(String a, String b, String c) { }
        @Override public int describeContents() { return 0; }
        @Override public void writeToParcel(Parcel parcel, int flags) { }
    }
    public static final class Decisions {
        public static final boolean exact(Context c, AdSource source, String tag) { return true; }
        public static boolean nonFinal(Context c, AdSource source, String tag) { return true; }
        public final boolean instance(Context c, AdSource source, String tag) { return true; }
        public static final Boolean boxed(Context c, AdSource source, String tag) { return true; }
        public static final boolean generic(Context c, Object source, String tag) { return true; }
        public static final native boolean nativeMethod(Context c, AdSource source, String tag);
    }
    private Method method(String name) throws Exception {
        return Decisions.class.getDeclaredMethod(name, Context.class, AdSource.class, String.class);
    }
    private ResolvedTarget target(String source, String descriptor) {
        return new ResolvedTarget(SplashDiagnosticResolver.DECISION, source,
                DescriptorUtils.classDescriptorOf(Decisions.class), descriptor);
    }
    @Test public void strictDecisionShapeAcceptsOnlyPrimitiveStaticFinalBusinessContract() throws Exception {
        assertTrue(SplashDiagnosticResolver.decisionShape(method("exact")));
        for (String name : new String[]{"nonFinal", "instance", "boxed", "nativeMethod"})
            assertFalse(name, SplashDiagnosticResolver.decisionShape(method(name)));
        assertFalse(SplashDiagnosticResolver.decisionShape(Decisions.class.getDeclaredMethod(
                "generic", Context.class, Object.class, String.class)));
    }
    @Test public void cachedTargetRequiresSemanticSchemaAndExactReturnDescriptor() throws Exception {
        String descriptor = org.luckypray.dexkit.util.DexSignUtil.getDescriptor(method("exact"));
        ClassLoader loader = getClass().getClassLoader();
        assertTrue(SplashDiagnosticResolver.verify(target(SplashDiagnosticResolver.SOURCE, descriptor), loader));
        assertFalse(SplashDiagnosticResolver.verify(target("legacy_name", descriptor), loader));
        assertFalse(SplashDiagnosticResolver.verify(target(SplashDiagnosticResolver.SOURCE,
                descriptor.substring(0, descriptor.length()-1) + "V"), loader));
    }
    @Test public void productionCacheCannotPromoteAnObservationOrMalformedDescriptor() throws Exception {
        String descriptor = org.luckypray.dexkit.util.DexSignUtil.getDescriptor(method("exact"));
        ClassLoader loader = getClass().getClassLoader();
        ResolvedTarget production = new ResolvedTarget(TargetResolver.KEY_SPLASH_DECISION,
                SplashDecisionResolver.SOURCE, DescriptorUtils.classDescriptorOf(Decisions.class), descriptor);
        assertNull(TargetVerifier.verify(production, loader));
        assertFalse(SplashDecisionResolver.verify(target(SplashDiagnosticResolver.SOURCE, descriptor), loader));
        assertNotNull(TargetVerifier.verify(new ResolvedTarget(production.key, "legacy_name",
                production.classDescriptor, descriptor), loader));
        assertNotNull(TargetVerifier.verify(new ResolvedTarget(production.key, production.source,
                production.classDescriptor, descriptor.substring(0, descriptor.length()-1) + "V"), loader));
        assertNotNull(TargetVerifier.verify(new ResolvedTarget(production.key, production.source,
                DescriptorUtils.classDescriptorOf(AdSource.class), descriptor), loader));
        assertNotNull(TargetVerifier.verify(new ResolvedTarget(production.key, production.source,
                production.classDescriptor, ""), loader));
    }
    @Test public void linkageFailureCannotAbortExistingHooksOrCertifyLegacyOnlyCoverage() {
        assertFalse(SplashDecisionResolver.hasEmbeddedHost(getClass().getClassLoader()));
        ClassLoader broken = new ClassLoader() {
            @Override protected Class<?> loadClass(String name, boolean resolve) {
                throw new NoClassDefFoundError("staged fragment dependency");
            }
        };
        assertTrue(SplashDecisionResolver.hasEmbeddedHost(broken));
        assertNull(SplashDecisionResolver.resolve(null, getClass().getClassLoader(), new ModuleLog(null)));
    }
    @Test public void observationWindowExpiresIndependentlyOfBootstrapReady() {
        SplashObservationBudget b = new SplashObservationBudget(100);
        assertTrue(b.take(100));
        assertTrue(b.take(59999));
        assertFalse(b.take(60100));
        assertFalse(b.active(60101));
    }
    @Test public void observationCountIsHardBoundedAndNeverRearms() {
        SplashObservationBudget b = new SplashObservationBudget(10);
        for (int i = 0; i < SplashObservationBudget.MAX_EVENTS; i++) assertTrue(b.take(11));
        assertFalse(b.take(12));
        b.close();
        assertFalse(b.take(10));
    }
    @Test public void stoppingOrInvalidClockRejectsMoreEvents() {
        SplashObservationBudget b = new SplashObservationBudget(20);
        assertFalse(b.take(19));
        assertTrue(b.take(20));
        b.close();
        assertFalse(b.take(21));
    }
}
