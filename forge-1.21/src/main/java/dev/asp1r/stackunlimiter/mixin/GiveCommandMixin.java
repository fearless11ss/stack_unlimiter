package dev.asp1r.stackunlimiter.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.asp1r.stackunlimiter.api.StackCounts;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.server.commands.GiveCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.Collection;

@Mixin(GiveCommand.class)
public abstract class GiveCommandMixin {
    @ModifyConstant(method = "giveItem", constant = @Constant(intValue = 100))
    private static int stackUnlimiter$avoidMaximumOverflow(
            int original,
            CommandSourceStack source,
            ItemInput item,
            Collection<ServerPlayer> targets,
            int count
    ) throws CommandSyntaxException {
        ItemStack prototype = item.createItemStack(1, false);
        return StackCounts.isEligible(prototype) ? 1 : original;
    }
}
