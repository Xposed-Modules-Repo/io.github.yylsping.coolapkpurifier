package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public final class CachePolicyTest {
    @Test
    public void evictsOldestBeyondFiveEntries() {
        List<CachePolicy.Record> records = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            records.add(new CachePolicy.Record("k" + i, i, 100));
        }
        List<CachePolicy.Record> kept = CachePolicy.evict(records, "k6");
        assertEquals(5, kept.size());
        assertTrue(kept.stream().noneMatch(record -> record.key.equals("k1")));
        assertTrue(kept.stream().anyMatch(record -> record.key.equals("k6")));
    }

    @Test
    public void evictsLruUntilBelowOneMiB() {
        List<CachePolicy.Record> records = new ArrayList<>(Arrays.asList(
                new CachePolicy.Record("a", 1, 700_000),
                new CachePolicy.Record("b", 2, 700_000),
                new CachePolicy.Record("protect", 3, 100_000)));
        List<CachePolicy.Record> kept = CachePolicy.evict(records, "protect");
        assertTrue(CachePolicy.totalBytes(kept) <= CachePolicy.MAX_TOTAL_BYTES);
        assertTrue(kept.stream().anyMatch(record -> record.key.equals("protect")));
    }

    @Test
    public void refusesOversizedSingleEntry() {
        List<CachePolicy.Record> records = new ArrayList<>(Arrays.asList(
                new CachePolicy.Record("huge", 1, CachePolicy.MAX_TOTAL_BYTES + 1),
                new CachePolicy.Record("small", 2, 10)));
        List<CachePolicy.Record> kept = CachePolicy.evict(records, "huge");
        assertTrue(kept.stream().noneMatch(record -> record.key.equals("huge")));
    }
}
