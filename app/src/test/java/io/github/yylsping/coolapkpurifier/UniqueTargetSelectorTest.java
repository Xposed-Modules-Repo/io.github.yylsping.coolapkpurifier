package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public final class UniqueTargetSelectorTest {
    @Test
    public void acceptsExactlyOneCandidate() {
        assertEquals("only", UniqueTargetSelector.only(
                Collections.singletonList("only")));
    }

    @Test
    public void rejectsMissingOrAmbiguousCandidates() {
        assertNull(UniqueTargetSelector.only(Collections.emptyList()));
        assertNull(UniqueTargetSelector.only(Arrays.asList("first", "second")));
        assertNull(UniqueTargetSelector.only(null));
    }
}
