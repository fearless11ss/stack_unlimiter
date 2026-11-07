package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Inventory.class, remap = false)
public abstract class InventoryMixin {
    @Inject(
            method = "getSlotWithRemainingSpace",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stackUnlimiter$findExactMergeSlot(
            ItemStack incoming,
            CallbackInfoReturnable<Integer> callback
    ) {
        if (!StackCounts.isEligible(incoming)) {
            return;
        }

        int mergeSlot = stackUnlimiter$findMergeSlot((Inventory) (Object) this, incoming);
        if (mergeSlot >= 0) {
            callback.setReturnValue(mergeSlot);
        }
    }

    @Inject(
            method = "add(ILnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stackUnlimiter$addExactStack(
            int requestedSlot,
            ItemStack incoming,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (incoming.isEmpty() || !StackCounts.isEligible(incoming)) {
            return;
        }

        Inventory inventory = (Inventory) (Object) this;
        int targetSlot = requestedSlot >= 0
                ? requestedSlot
                : stackUnlimiter$findTargetSlot(inventory, incoming);
        if (targetSlot < 0) {
            return;
        }

        ItemStack target = inventory.getItem(targetSlot);
        long incomingCount = StackCounts.get(incoming);
        if (target.isEmpty()) {
            // Some vanilla insertion paths zero the int facade before calling
            // this overload. Restore it from the exact count so copy() does
            // not mistake the source for an empty stack.
            StackCounts.set(incoming, incomingCount);
            ItemStack placed = incoming.copy();
            StackCounts.set(incoming, 0);
            StackCounts.set(placed, incomingCount);
            inventory.setItem(targetSlot, placed);
            placed.setPopTime(5);
            callback.setReturnValue(true);
            return;
        }

        if (!StackCounts.sameItemSameComponents(target, incoming)) {
            return;
        }

        long room = Long.MAX_VALUE - StackCounts.get(target);
        long moved = Math.min(room, incomingCount);
        if (moved == 0) {
            callback.setReturnValue(false);
            return;
        }

        StackCounts.grow(target, moved);
        StackCounts.shrink(incoming, moved);
        target.setPopTime(5);
        callback.setReturnValue(true);
    }

    private static int stackUnlimiter$findTargetSlot(Inventory inventory, ItemStack incoming) {
        int mergeSlot = stackUnlimiter$findMergeSlot(inventory, incoming);
        return mergeSlot >= 0 ? mergeSlot : inventory.getFreeSlot();
    }

    private static int stackUnlimiter$findMergeSlot(Inventory inventory, ItemStack incoming) {
        int selected = inventory.getSelectedSlot();
        if (stackUnlimiter$canMerge(inventory.getItem(selected), incoming)) {
            return selected;
        }
        if (stackUnlimiter$canMerge(inventory.getItem(40), incoming)) {
            return 40;
        }
        for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
            if (stackUnlimiter$canMerge(inventory.getNonEquipmentItems().get(slot), incoming)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean stackUnlimiter$canMerge(ItemStack target, ItemStack incoming) {
        return !target.isEmpty()
                && StackCounts.sameItemSameComponents(target, incoming)
                && StackCounts.get(target) < Long.MAX_VALUE;
    }
}
