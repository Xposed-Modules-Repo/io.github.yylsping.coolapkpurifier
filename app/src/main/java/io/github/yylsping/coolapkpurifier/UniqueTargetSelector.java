package io.github.yylsping.coolapkpurifier;

import java.util.List;

/** Rejects zero or ambiguous semantic candidates instead of guessing. */
final class UniqueTargetSelector {
    private UniqueTargetSelector() {
    }

    static <T> T only(List<T> candidates) {
        return candidates != null && candidates.size() == 1
                ? candidates.get(0) : null;
    }
}
