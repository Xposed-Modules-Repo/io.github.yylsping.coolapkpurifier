package io.github.yylsping.coolapkpurifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure LRU/size eviction policy. Unit-testable without Android. */
final class CachePolicy {
    static final int MAX_ENTRIES = 5;
    static final int MAX_TOTAL_BYTES = 1024 * 1024;

    private CachePolicy() {
    }

    static final class Record {
        final String key;
        final long lastUsedAt;
        final long sizeBytes;

        Record(String key, long lastUsedAt, long sizeBytes) {
            this.key = key;
            this.lastUsedAt = lastUsedAt;
            this.sizeBytes = sizeBytes;
        }
    }

    static List<Record> evict(List<Record> records, String protectKey) {
        List<Record> working = new ArrayList<>(records);
        // Drop records that already exceed the hard budget individually; they
        // can never be persisted.
        working.removeIf(record -> record.sizeBytes > MAX_TOTAL_BYTES);

        while (working.size() > MAX_ENTRIES || totalBytes(working) > MAX_TOTAL_BYTES) {
            Record victim = null;
            for (Record record : working) {
                if (protectKey != null && protectKey.equals(record.key)) {
                    continue;
                }
                if (victim == null || record.lastUsedAt < victim.lastUsedAt) {
                    victim = record;
                }
            }
            if (victim == null) {
                // Every record is protected or list is empty. Drop the oldest
                // anyway rather than exceeding the hard limit.
                victim = working.stream()
                        .min(Comparator.comparingLong(record -> record.lastUsedAt))
                        .orElse(null);
            }
            if (victim == null) {
                break;
            }
            working.remove(victim);
        }
        return working;
    }

    static long totalBytes(List<Record> records) {
        long total = 0L;
        for (Record record : records) {
            total += record.sizeBytes;
        }
        return total;
    }
}
