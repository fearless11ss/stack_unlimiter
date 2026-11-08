package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemStackHandler.class, remap = false)
public abstract class ItemStackHandlerMixin {
    @Inject(method = "getSlotLimit", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$getUnlimitedSlotLimit(int slot, CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(Integer.MAX_VALUE);
    }

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$insertExactStack(
            int slot,
            ItemStack incoming,
            boolean simulate,
            CallbackInfoReturnable<ItemStack> callback
    ) {
        if (incoming.isEmpty() || !StackCounts.isEligible(incoming)) {
            return;
        }

        ItemStackHandler handler = (ItemStackHandler) (Object) this;
        ItemStack existing = handler.getStackInSlot(slot);
        if ((existing.isEmpty() || StackCounts.get(existing) <= existing.getCount())
                && StackCounts.get(incoming) <= incoming.getCount()) {
            return;
        }
        if (!handler.isItemValid(slot, incoming)
                || (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, incoming))) {
            callback.setReturnValue(incoming);
            return;
        }

        long incomingCount = StackCounts.get(incoming);
        long room = existing.isEmpty() ? Long.MAX_VALUE : Long.MAX_VALUE - StackCounts.get(existing);
        long moved = Math.min(room, incomingCount);
        if (!simulate && moved > 0) {
            if (existing.isEmpty()) {
                handler.setStackInSlot(slot, StackCounts.copyWithCount(incoming, moved));
            } else {
                StackCounts.grow(existing, moved);
                handler.setStackInSlot(slot, existing);
            }
        }

        callback.setReturnValue(StackCounts.copyWithCount(incoming, incomingCount - moved));
    }

    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$extractExactStack(
            int slot,
            int amount,
            boolean simulate,
            CallbackInfoReturnable<ItemStack> callback
    ) {
        if (amount <= 0) {
            return;
        }

        ItemStackHandler handler = (ItemStackHandler) (Object) this;
        ItemStack existing = handler.getStackInSlot(slot);
        if (existing.isEmpty() || StackCounts.get(existing) <= existing.getCount()) {
            return;
        }

        long moved = Math.min((long) amount, StackCounts.get(existing));
        ItemStack extracted = StackCounts.copyWithCount(existing, moved);
        if (!simulate) {
            StackCounts.shrink(existing, moved);
            handler.setStackInSlot(slot, existing.isEmpty() ? ItemStack.EMPTY : existing);
        }
        callback.setReturnValue(extracted);
    }
}
