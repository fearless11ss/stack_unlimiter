package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StackedContents.class)
public abstract class StackedContentsMixin {
    @Shadow
    public abstract void accountStack(ItemStack stack, int maximum);

    @Shadow
    abstract void put(int stackingIndex, int count);

    @Inject(method = "accountStack(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$accountExactStack(ItemStack stack, CallbackInfo callback) {
        if (!StackCounts.isEligible(stack)) {
            return;
        }

        this.accountStack(stack, Integer.MAX_VALUE);
        callback.cancel();
    }

    @Inject(
            method = "accountStack(Lnet/minecraft/world/item/ItemStack;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stackUnlimiter$accountExactStackWithMaximum(
            ItemStack stack,
            int maximum,
            CallbackInfo callback
    ) {
        if (!StackCounts.isEligible(stack)) {
            return;
        }

        long exactCount = StackCounts.get(stack);
        int recipeCount = (int) Math.min(exactCount, Math.max(0, maximum));
        this.put(StackedContents.getStackingIndex(stack), recipeCount);
        callback.cancel();
    }
}
