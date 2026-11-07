package dev.asp1r.stackunlimiter.mixin;

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
}
