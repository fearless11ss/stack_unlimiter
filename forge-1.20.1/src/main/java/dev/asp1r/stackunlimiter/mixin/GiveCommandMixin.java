package dev.asp1r.stackunlimiter.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.server.commands.GiveCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

@Mixin(GiveCommand.class)
public abstract class GiveCommandMixin {
    @Redirect(
            method = "giveItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;getMaxStackSize()I")
    )
    private static int stackUnlimiter$useOneEligibleStack(Item item) {
        int vanillaMaximum = item.getMaxStackSize();
        return vanillaMaximum > 1 ? Integer.MAX_VALUE : vanillaMaximum;
    }

    @ModifyConstant(method = "giveItem", constant = @Constant(intValue = 100))
    private static int stackUnlimiter$avoidMaximumOverflow(
            int original,
            CommandSourceStack source,
            ItemInput item,
            Collection<ServerPlayer> targets,
            int count
    ) {
        return item.getItem().getMaxStackSize() > 1 ? 1 : original;
    }
}
