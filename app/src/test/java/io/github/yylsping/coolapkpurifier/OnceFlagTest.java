package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class OnceFlagTest {
    @Test
    public void onlyFirstCallWins() {
        OnceFlag flag = new OnceFlag();
        assertTrue(flag.tryOnce());
        assertFalse(flag.tryOnce());
        assertFalse(flag.tryOnce());
    }
}
