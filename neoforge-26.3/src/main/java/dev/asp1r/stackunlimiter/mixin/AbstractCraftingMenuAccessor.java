package dev.asp1r.stackunlimiter.mixin;

import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractCraftingMenu.class, remap = false)
public interface AbstractCraftingMenuAccessor {
    @Accessor("craftSlots")
    CraftingContainer stackUnlimiter$getCraftSlots();

    @Accessor("resultSlots")
    ResultContainer stackUnlimiter$getResultSlots();
}
