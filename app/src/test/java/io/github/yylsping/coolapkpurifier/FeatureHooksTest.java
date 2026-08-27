package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public final class FeatureHooksTest {
    @Test
    public void relatedGetterSuppressesOnlyItsReturnedListWithoutMutatingModel() {
        List<String> original = Arrays.asList("recommendation", "another");
        assertTrue(((List<?>) FeatureHooks.filterRelatedData(original, true)).isEmpty());
        assertEquals(Arrays.asList("recommendation", "another"), original);
        assertSame(original, FeatureHooks.filterRelatedData(original, false));
    }

    @Test
    public void relatedGetterPreservesNullEmptyAndUnexpectedValues() {
        assertNull(FeatureHooks.filterRelatedData(null, true));
        List<?> empty = Collections.emptyList();
        assertSame(empty, FeatureHooks.filterRelatedData(empty, true));
        Object unexpected = new Object();
        assertSame(unexpected, FeatureHooks.filterRelatedData(unexpected, true));
    }
}
