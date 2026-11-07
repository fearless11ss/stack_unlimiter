package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = InventoryMenu.class, remap = false)
public abstract class InventoryMenuMixin {
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$bulkCraftResult(
            Player player,
            int slotIndex,
            CallbackInfoReturnable<ItemStack> callback
    ) {
        if (slotIndex != 0) {
            return;
        }

        AbstractCraftingMenuAccessor menu = (AbstractCraftingMenuAccessor) this;
        CraftingContainer craftSlots = menu.stackUnlimiter$getCraftSlots();
        ResultContainer resultSlots = menu.stackUnlimiter$getResultSlots();
        ItemStack singleResult = resultSlots.getItem(0);
        if (singleResult.isEmpty() || !StackCounts.isEligible(singleResult)) {
            return;
        }

        Optional<RecipeHolder<CraftingRecipe>> recipeMatch = player.level()
                .getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftSlots.asCraftInput(), player.level());
        if (recipeMatch.isEmpty()) {
            return;
        }
        CraftingRecipe recipe = recipeMatch.get().value();

        NonNullList<ItemStack> remainingItems;
        ForgeHooks.setCraftingPlayer(player);
        try {
            remainingItems = recipe.getRemainingItems(craftSlots.asCraftInput());
        } finally {
            ForgeHooks.setCraftingPlayer(null);
        }
        if (remainingItems.stream().anyMatch(stack -> !stack.isEmpty())) {
            return;
        }

        long craftCount = Long.MAX_VALUE;
        boolean hasIngredient = false;
        for (ItemStack ingredient : craftSlots.getItems()) {
            if (!ingredient.isEmpty()) {
                hasIngredient = true;
                craftCount = Math.min(craftCount, StackCounts.get(ingredient));
            }
        }
        if (!hasIngredient || craftCount <= 1) {
            return;
        }

        long resultPerCraft = StackCounts.get(singleResult);
        final long totalResult;
        try {
            totalResult = Math.multiplyExact(craftCount, resultPerCraft);
        } catch (ArithmeticException exception) {
            return;
        }

        Inventory inventory = player.getInventory();
        int destination = stackUnlimiter$findDestination(inventory, singleResult, totalResult);
        if (destination < 0) {
            return;
        }

        ItemStack crafted = singleResult.copy();
        StackCounts.set(crafted, totalResult);
        crafted.onCraftedBy(player, (int) Math.min(totalResult, Integer.MAX_VALUE));
        ForgeEventFactory.firePlayerCraftingEvent(player, crafted, craftSlots);
        resultSlots.awardUsedRecipes(player, craftSlots.getItems());

        for (ItemStack ingredient : craftSlots.getItems()) {
            if (!ingredient.isEmpty()) {
                StackCounts.shrink(ingredient, craftCount);
            }
        }
        craftSlots.setChanged();
        resultSlots.setItem(0, ItemStack.EMPTY);

        ItemStack destinationStack = inventory.getItem(destination);
        if (destinationStack.isEmpty()) {
            inventory.setItem(destination, crafted);
        } else {
            StackCounts.grow(destinationStack, totalResult);
        }
        inventory.setChanged();
        callback.setReturnValue(ItemStack.EMPTY);
    }

    private static int stackUnlimiter$findDestination(
            Inventory inventory,
            ItemStack result,
            long resultCount
    ) {
        for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
            ItemStack target = inventory.getNonEquipmentItems().get(slot);
            if (!target.isEmpty()
                    && StackCounts.sameItemSameComponents(target, result)
                    && Long.MAX_VALUE - StackCounts.get(target) >= resultCount) {
                return slot;
            }
        }
        return inventory.getFreeSlot();
    }
}
