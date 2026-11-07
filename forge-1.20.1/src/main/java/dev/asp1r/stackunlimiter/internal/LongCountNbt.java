package dev.asp1r.stackunlimiter.internal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public final class LongCountNbt {
    private static final String PERSISTENT_KEY = "stack_unlimiter:data";
    private static final String NETWORK_KEY = "stack_unlimiter:network";
    private static final String VERSION_KEY = "version";
    private static final String COUNT_KEY = "count";
    private static final int FORMAT_VERSION = 1;

    private LongCountNbt() {
    }

    public static void writePersistent(CompoundTag stackTag, long exactCount, int vanillaMaximum) {
        int safeCount = LongCountMath.safeSerializedCount(exactCount, vanillaMaximum);
        stackTag.putByte("Count", (byte) safeCount);

        if (exactCount == safeCount) {
            stackTag.remove(PERSISTENT_KEY);
            return;
        }

        stackTag.put(PERSISTENT_KEY, createPayload(exactCount));
    }

    public static @Nullable Long readPersistent(CompoundTag stackTag) {
        return readPayload(stackTag, PERSISTENT_KEY, false);
    }

    public static CompoundTag attachNetworkCount(@Nullable CompoundTag original, long exactCount) {
        CompoundTag result = original == null ? new CompoundTag() : original.copy();
        result.put(NETWORK_KEY, createPayload(exactCount));
        return result;
    }

    public static @Nullable Long removeAndReadNetworkCount(@Nullable CompoundTag networkTag) {
        if (networkTag == null) {
            return null;
        }
        return readPayload(networkTag, NETWORK_KEY, true);
    }

    private static CompoundTag createPayload(long exactCount) {
        LongCountMath.requireValid(exactCount);
        CompoundTag payload = new CompoundTag();
        payload.putInt(VERSION_KEY, FORMAT_VERSION);
        payload.putLong(COUNT_KEY, exactCount);
        return payload;
    }

    private static @Nullable Long readPayload(CompoundTag parent, String key, boolean remove) {
        if (!parent.contains(key, Tag.TAG_COMPOUND)) {
            return null;
        }

        CompoundTag payload = parent.getCompound(key);
        if (remove) {
            parent.remove(key);
        }
        int version = payload.getInt(VERSION_KEY);
        if (version != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported Stack Unlimiter data version: " + version);
        }
        if (!payload.contains(COUNT_KEY, Tag.TAG_LONG)) {
            throw new IllegalArgumentException("Stack Unlimiter count is missing or has the wrong type");
        }
        return LongCountMath.requireValid(payload.getLong(COUNT_KEY));
    }
}
