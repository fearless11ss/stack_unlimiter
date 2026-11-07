package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StackedItemContents.class, remap = false)
public abstract class StackedContentsMixin {
    @Shadow
    @Final
    private StackedContents<Holder<Item>> raw;

    @Inject(
            method = "accountStack(Lnet/minecraft/world/item/ItemStack;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stackUnlimiter$accountExactStack(
            ItemStack stack,
            int maximum,
            CallbackInfo callback
    ) {
        if (!StackCounts.isEligible(stack)) {
            return;
        }

        long exactCount = StackCounts.get(stack);
        int recipeCount = (int) Math.min(exactCount, maximum);
        this.raw.account(stack.typeHolder(), recipeCount);
        callback.cancel();
    }
}
