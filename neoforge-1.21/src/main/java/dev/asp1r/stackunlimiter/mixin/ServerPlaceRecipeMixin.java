package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import dev.asp1r.stackunlimiter.internal.LongCountMath;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerPlaceRecipe.class, remap = false)
public abstract class ServerPlaceRecipeMixin {
    @Redirect(
            method = "moveItemToGrid",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I")
    )
    private int stackUnlimiter$moveExactIngredientCount(ItemStack stack) {
        return LongCountMath.facade(StackCounts.get(stack));
    }
}
