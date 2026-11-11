package dev.asp1r.stackunlimiter.api;

import dev.asp1r.stackunlimiter.internal.LongCountAccess;
import dev.asp1r.stackunlimiter.internal.LongCountMath;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
        if (get(stack) == 0) {
            return ItemStack.EMPTY;
        }
        // Vanilla Inventory.add creates a typed zero-count placeholder and
        // grows it after placing it into an empty slot. Returning the global
        // ItemStack.EMPTY singleton here would make vanilla grow that shared
        // singleton and silently lose non-stackable items such as tools.
        ItemStack result = stack.copy();
        set(result, count);
        return result;
    }

    public static boolean isEligible(ItemStack stack) {
        return stack != ItemStack.EMPTY
                && stack.getItem() != Items.AIR
                && vanillaMaxStackSize(stack) > 1;
    }

    public static int vanillaMaxStackSize(ItemStack stack) {
        if (stack == ItemStack.EMPTY || stack.getItem() == Items.AIR) {
            return 1;
        }
        return Math.max(1, stack.getOrDefault(DataComponents.MAX_STACK_SIZE, 1));
    }

    /**
     * Compares item type and components while treating the internal exact-count
     * marker as stack metadata rather than item identity.
     */
    public static boolean sameItemSameComponents(ItemStack first, ItemStack second) {
        if (!first.is(second.getItem())) {
            return false;
        }
        if (first.isEmpty() && second.isEmpty()) {
            return true;
        }

        Set<DataComponentType<?>> componentTypes = new HashSet<>(first.getComponents().keySet());
        componentTypes.addAll(second.getComponents().keySet());
        for (DataComponentType<?> componentType : componentTypes) {
            if (componentType == DataComponents.CUSTOM_DATA) {
                if (!Objects.equals(customDataWithoutExactCount(first), customDataWithoutExactCount(second))) {
                    return false;
                }
            } else if (!Objects.equals(
                    first.getComponents().get(componentType),
                    second.getComponents().get(componentType)
            )) {
                return false;
            }
        }
        return true;
    }

    private static CompoundTag customDataWithoutExactCount(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData == null ? new CompoundTag() : customData.copyTag();
        tag.remove(LongCountAccess.EXACT_COUNT_KEY);
        return tag;
    }

    private static LongCountAccess access(ItemStack stack) {
        try {
            return (LongCountAccess) (Object) stack;
        } catch (ClassCastException exception) {
            throw new IllegalStateException("Stack Unlimiter mixin is not active", exception);
        }
    }
}
