package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Redirect(
            method = {
                    "renderSlot",
                    "recalculateQuickCraftRemaining",
                    "mouseDragged"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I")
    )
    private int stackUnlimiter$getExactQuickCraftPreviewCount(ItemStack stack) {
        return (int) Math.min(StackCounts.get(stack), Integer.MAX_VALUE);
    }
}
