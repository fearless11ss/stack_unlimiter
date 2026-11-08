package dev.asp1r.stackunlimiter.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BulkCraftingDispatchTest {
    @Test
    void clientResultClickIsDeferredToTheServer() {
        assertTrue(BulkCraftingDispatch.shouldDeferResultClickToServer(true, 0));
    }

    @Test
    void serverAndOrdinarySlotsAreNotDeferred() {
        assertFalse(BulkCraftingDispatch.shouldDeferResultClickToServer(false, 0));
        assertFalse(BulkCraftingDispatch.shouldDeferResultClickToServer(true, 1));
    }
}


