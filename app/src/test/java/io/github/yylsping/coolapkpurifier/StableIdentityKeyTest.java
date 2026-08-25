package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.util.Arrays;

public final class StableIdentityKeyTest {
    @Test
    public void reinstallWithDifferentPathProducesSameKey() {
        String first = StableIdentityKey.compute(
                "com.coolapk.market", 2_608_212L, 99_841_375L,
                new long[]{1_000L, 2_000L}, "cert-a");
        String second = StableIdentityKey.compute(
                "com.coolapk.market", 2_608_212L, 99_841_375L,
                new long[]{1_000L, 2_000L}, "cert-a");
        assertEquals(first, second);
    }

    @Test
    public void differentCodeSizeProducesDifferentKey() {
        String first = StableIdentityKey.compute(
                "com.coolapk.market", 2_608_212L,
                99_841_375L, new long[0], "cert-a");
        String second = StableIdentityKey.compute(
                "com.coolapk.market", 2_608_212L,
                101_688_539L, new long[0], "cert-a");
        assertNotEquals(first, second);
    }

    @Test
    public void differentVersionCodeCannotCollideAtTheSameSize() {
        assertNotEquals(
                StableIdentityKey.compute("pkg", 100L, 123L, null, "cert"),
                StableIdentityKey.compute("pkg", 101L, 123L, null, "cert"));
    }

    @Test
    public void sameVersionAndBuildFactsRemainStable() {
        assertEquals(
                StableIdentityKey.compute("pkg", 100L, 123L, null, "cert"),
                StableIdentityKey.compute("pkg", 100L, 123L, null, "cert"));
    }

    @Test
    public void differentSignerCannotCollideAtTheSameVersionAndSize() {
        assertNotEquals(
                StableIdentityKey.compute("pkg", 100L, 123L, null, "cert-a"),
                StableIdentityKey.compute("pkg", 100L, 123L, null, "cert-b"));
    }

    @Test
    public void splitIdentityIsStableWhenSystemOrderChanges() {
        String first = StableIdentityKey.compute("pkg", 100L, "base.apk#123",
                Arrays.asList("split_config.arm64.apk#20", "split_config.zh.apk#10"),
                "cert");
        String second = StableIdentityKey.compute("pkg", 100L, "base.apk#123",
                Arrays.asList("split_config.zh.apk#10", "split_config.arm64.apk#20"),
                "cert");
        assertEquals(first, second);
    }

    @Test
    public void equalSizedDifferentSplitNamesDoNotCollide() {
        assertNotEquals(
                StableIdentityKey.compute("pkg", 100L, "base.apk#123",
                        Arrays.asList("split_a.apk#20"), "cert"),
                StableIdentityKey.compute("pkg", 100L, "base.apk#123",
                        Arrays.asList("split_b.apk#20"), "cert"));
    }
}
