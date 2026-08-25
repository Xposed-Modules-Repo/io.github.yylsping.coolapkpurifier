package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.nio.charset.StandardCharsets;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.junit.Test;

public final class TargetIdentityTest {
    @Test
    public void singleSignerHashesCertificateBytesDirectly() {
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                TargetIdentity.signerDigest(new byte[][]{
                        "abc".getBytes(StandardCharsets.UTF_8)
                }));
    }

    @Test
    public void signerSetDigestIsStableAndOrderIndependent() {
        byte[] first = {1, 2, 3};
        byte[] second = {4, 5, 6};

        assertEquals(TargetIdentity.signerDigest(new byte[][]{first, second}),
                TargetIdentity.signerDigest(new byte[][]{second, first}));
    }

    @Test
    public void differentSignerCertificatesProduceDifferentDigests() {
        assertNotEquals(TargetIdentity.signerDigest(new byte[][]{{1, 2, 3}}),
                TargetIdentity.signerDigest(new byte[][]{{1, 2, 4}}));
    }

    @Test
    public void missingSigningInfoIsExplicitlyUnavailable() {
        assertEquals(TargetIdentity.SIGNING_UNAVAILABLE,
                TargetIdentity.signerDigest(null));
        assertEquals(TargetIdentity.SIGNING_UNAVAILABLE,
                TargetIdentity.signerDigest(new byte[0][]));
    }

    @Test
    public void signerQueryFailureDoesNotDiscardVersionFacts() {
        PackageInfo basic = new PackageInfo();
        basic.versionCode = 2608212;
        basic.versionName = "16.6.1";

        TargetIdentity.PackageFacts facts = TargetIdentity.readPackageFacts(flags -> {
            if (flags == PackageManager.GET_SIGNING_CERTIFICATES) {
                throw new IllegalStateException("signer unavailable");
            }
            return basic;
        });

        assertEquals(2608212L, facts.versionCode);
        assertEquals("16.6.1", facts.versionName);
        assertEquals(TargetIdentity.SIGNING_UNAVAILABLE, facts.signingHash);
    }
}
