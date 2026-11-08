package dev.asp1r.stackunlimiter.internal;

public final class BulkCraftingDispatch {
    private BulkCraftingDispatch() {
    }

    public static boolean shouldDeferResultClickToServer(boolean clientSide, int slotIndex) {
        return clientSide && slotIndex == 0;
    }
}


