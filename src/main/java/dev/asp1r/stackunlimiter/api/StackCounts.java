package dev.asp1r.stackunlimiter.api;

import dev.asp1r.stackunlimiter.internal.LongCountAccess;
import dev.asp1r.stackunlimiter.internal.LongCountMath;
import net.minecraft.world.item.ItemStack;

/**
 * Public API for exact stack counts. Callers must not infer an exact count from
 * {@link ItemStack#getCount()} once a stack can exceed {@link Integer#MAX_VALUE}.
 */
public final class StackCounts {
    private StackCounts() {
    }

    public static long get(ItemStack stack) {
        try {
            return ((LongCountAccess) (Object) stack).stackUnlimiter$getLongCount();
        } catch (ClassCastException exception) {
            throw new IllegalStateException("Stack Unlimiter mixin is not active", exception);
        }
    }

    public static void set(ItemStack stack, long count) {
        LongCountMath.requireValid(count);
        if (stack == ItemStack.EMPTY && count != 0) {
            throw new IllegalArgumentException("ItemStack.EMPTY cannot hold items");
        }
        if (count > 1 && !isEligible(stack)) {
            throw new IllegalArgumentException("This item is intentionally non-stackable");
        }
        access(stack).stackUnlimiter$setLongCountUnchecked(count);
    }

    public static void grow(ItemStack stack, long amount) {
        set(stack, LongCountMath.addExactNonNegative(get(stack), amount));
    }

    public static void shrink(ItemStack stack, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Shrink amount must be non-negative");
        }
        set(stack, LongCountMath.subtractFloorZero(get(stack), amount));
    }

    public static ItemStack split(ItemStack stack, long requested) {
        if (requested <= 0 || get(stack) == 0) {
            return ItemStack.EMPTY;
        }

        long moved = Math.min(requested, get(stack));
        ItemStack result = stack.copy();
        set(result, moved);
        shrink(stack, moved);
        return result;
    }

    public static ItemStack copyWithCount(ItemStack stack, long count) {
        if (count == 0 || get(stack) == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.copy();
        set(result, count);
        return result;
    }

    public static boolean isEligible(ItemStack stack) {
        return stack != ItemStack.EMPTY && get(stack) > 0 && vanillaMaxStackSize(stack) > 1;
    }

    public static int vanillaMaxStackSize(ItemStack stack) {
        if (stack == ItemStack.EMPTY || get(stack) == 0) {
            return 1;
        }
        return Math.max(1, stack.getItem().getMaxStackSize(stack));
    }

    private static LongCountAccess access(ItemStack stack) {
        try {
            return (LongCountAccess) (Object) stack;
        } catch (ClassCastException exception) {
            throw new IllegalStateException("Stack Unlimiter mixin is not active", exception);
        }
    }
}
