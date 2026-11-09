package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {
    @Inject(method = "getMaxStackSize()I", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$getUnlimitedSlotMaximum(CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(Integer.MAX_VALUE);
    }

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$getUnlimitedSlotMaximum(
            ItemStack stack,
            CallbackInfoReturnable<Integer> callback
    ) {
        if (StackCounts.isEligible(stack)) {
            callback.setReturnValue(Integer.MAX_VALUE);
        }
    }
}
