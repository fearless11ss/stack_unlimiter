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
}
