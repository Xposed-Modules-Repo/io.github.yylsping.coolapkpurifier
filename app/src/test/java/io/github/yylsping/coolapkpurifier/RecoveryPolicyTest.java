package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class RecoveryPolicyTest {
    @Test
    public void markersAreTrimmedToRecentWindow() {
        List<String> markers = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            markers.add("identity-" + i);
        }
        List<String> trimmed = RecoveryPolicy.trim(markers);
        assertEquals(RecoveryPolicy.MAX_MARKERS, trimmed.size());
        assertEquals("identity-72", trimmed.get(0));
        assertEquals("identity-199", trimmed.get(trimmed.size() - 1));
    }
}
