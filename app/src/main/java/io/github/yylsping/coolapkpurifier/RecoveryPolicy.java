package io.github.yylsping.coolapkpurifier;

import java.util.ArrayList;
import java.util.List;

/** Recovery markers are bounded so the marker array can never grow forever. */
final class RecoveryPolicy {
    static final int MAX_MARKERS = 128;

    private RecoveryPolicy() {
    }

    static List<String> trim(List<String> markers) {
        List<String> result = new ArrayList<>(markers);
        while (result.size() > MAX_MARKERS) {
            result.remove(0);
        }
        return result;
    }
}
