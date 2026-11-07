package dev.asp1r.stackunlimiter.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackCountDisplayTest {
    @Test
    void abbreviatesTheReportedOverflowCase() {
        assertEquals("2K", StackCountDisplay.format(2_004));
    }

    @Test
    void scalesAnyLabelThatWouldOverflowASlot() {
        int unscaledWidth = 24;
        float scale = StackCountDisplay.scaleForWidth(unscaledWidth);

        assertTrue(unscaledWidth * scale <= StackCountDisplay.MAX_LABEL_WIDTH);
    }

    @Test
    void largeStackLabelUsesAnOpaqueColor() {
        assertEquals(0xFF, StackCountDisplay.TEXT_COLOR >>> 24);
        assertEquals("1M", StackCountDisplay.format(1_000_000));
    }
}
