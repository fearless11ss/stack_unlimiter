package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import dev.asp1r.stackunlimiter.internal.LongCountMath;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ServerPlaceRecipe.class, remap = false)
public abstract class ServerPlaceRecipeMixin {
    @Inject(method = "clampToMaxStackSize", at = @At("HEAD"), cancellable = true)
    private static void stackUnlimiter$keepUnlimitedRecipeAmount(
            int requested,
            List<Holder<Item>> ingredients,
            CallbackInfoReturnable<Integer> callback
    ) {
        int result = requested;
        for (Holder<Item> ingredient : ingredients) {
            int vanillaMaximum = ingredient.components().getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
            if (vanillaMaximum <= 1) {
                result = Math.min(result, vanillaMaximum);
            }
        }
        callback.setReturnValue(result);
    }

    @Redirect(
            method = "moveItemToGrid",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I")
    )
    private int stackUnlimiter$moveExactIngredientCount(ItemStack stack) {
        return LongCountMath.facade(StackCounts.get(stack));
    }

    @Redirect(
            method = "calculateAmountToCraft",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I")
    )
    private int stackUnlimiter$incrementExactIngredientCount(ItemStack stack) {
        return LongCountMath.facade(StackCounts.get(stack));
    }
}
