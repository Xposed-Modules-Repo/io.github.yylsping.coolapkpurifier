package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HookLedgerTest {

    @Test
    public void recordIsIdempotentPerId() {
        HookLedger ledger = new HookLedger();
        ledger.record(HookLedger.Layer.FRAMEWORK, "coordinator", "a", "T1");
        ledger.record(HookLedger.Layer.FRAMEWORK, "coordinator", "a", "T1-again");
        assertEquals(1, ledger.count());
        assertTrue(ledger.isActive("a"));
    }

    @Test
    public void retireClosesActiveEntryOnce() {
        HookLedger ledger = new HookLedger();
        ledger.record(HookLedger.Layer.FRAMEWORK, "splash", "i", "T");
        assertTrue(ledger.retire("i", "terminal:READY"));
        assertFalse(ledger.retire("i", "second"));
        assertFalse(ledger.isActive("i"));
        assertFalse(ledger.retire("unknown", "x"));
    }

    @Test
    public void activeIdsFilterByLayer() {
        HookLedger ledger = new HookLedger();
        ledger.record(HookLedger.Layer.FRAMEWORK, "o", "f1", "t");
        ledger.record(HookLedger.Layer.BUSINESS, "o", "b1", "t");
        ledger.record(HookLedger.Layer.BUSINESS, "o", "b2", "t");
        ledger.retire("b1", "done");
        assertEquals(java.util.Arrays.asList("f1"),
                ledger.activeIds(HookLedger.Layer.FRAMEWORK));
        assertEquals(java.util.Arrays.asList("b2"),
                ledger.activeIds(HookLedger.Layer.BUSINESS));
        assertTrue(ledger.hasActiveFrameworkHooks());
        ledger.retire("f1", "done");
        assertFalse(ledger.hasActiveFrameworkHooks());
    }

    @Test
    public void summaryLineReportsFrameworkState() {
        HookLedger ledger = new HookLedger();
        ledger.record(HookLedger.Layer.FRAMEWORK, "o", "fw", "t");
        ledger.record(HookLedger.Layer.BUSINESS, "o", "biz", "t");
        String active = ledger.summaryLine("READY");
        assertTrue(active.contains("hook ledger state=READY"));
        assertTrue(active.contains("frameworkActive=true"));
        assertTrue(active.contains("frameworkActiveHooks=[fw]"));
        assertTrue(active.contains("businessActiveHooks=[biz]"));
        ledger.retire("fw", "terminal:READY");
        String retired = ledger.summaryLine("READY");
        assertTrue(retired.contains("frameworkActive=false"));
        assertTrue(retired.contains("frameworkActiveHooks=[]"));
    }

    @Test
    public void describeListsRetireReason() {
        HookLedger ledger = new HookLedger();
        ledger.record(HookLedger.Layer.FRAMEWORK, "owner", "x", "target");
        ledger.retire("x", "because");
        String text = ledger.describe();
        assertTrue(text.contains("FRAMEWORK x"));
        assertTrue(text.contains("active=false"));
        assertTrue(text.contains("retiredBecause=because"));
    }
}
