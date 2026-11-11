package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxBlockMixin {
    @Inject(method = "appendHoverText", at = @At("RETURN"))
    private void stackUnlimiter$showExactContainedCounts(
            ItemStack shulker,
            BlockGetter level,
            List<Component> tooltip,
            TooltipFlag flag,
            CallbackInfo callback
    ) {
        CompoundTag blockEntityData = BlockItem.getBlockEntityData(shulker);
        if (blockEntityData == null || !blockEntityData.contains("Items", CompoundTag.TAG_LIST)) {
            return;
        }

        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(blockEntityData, contents);
        int searchFrom = 0;

        for (ItemStack contained : contents) {
            if (contained.isEmpty()) {
                continue;
            }

            String vanillaLine = contained.getHoverName().getString() + " x" + contained.getCount();
            int lineIndex = stackUnlimiter$findLine(tooltip, vanillaLine, searchFrom);
            if (lineIndex < 0) {
                continue;
            }
            searchFrom = lineIndex + 1;

            long exactCount = StackCounts.get(contained);
            if (exactCount == contained.getCount()) {
                continue;
            }

            MutableComponent replacement = contained.getHoverName().copy()
                    .append(" x")
                    .append(String.valueOf(exactCount));
            tooltip.set(lineIndex, replacement);
        }
    }

    private static int stackUnlimiter$findLine(
            List<Component> tooltip,
            String expected,
            int searchFrom
    ) {
        for (int index = searchFrom; index < tooltip.size(); index++) {
            if (expected.equals(tooltip.get(index).getString())) {
                return index;
            }
        }
        return -1;
    }
}
