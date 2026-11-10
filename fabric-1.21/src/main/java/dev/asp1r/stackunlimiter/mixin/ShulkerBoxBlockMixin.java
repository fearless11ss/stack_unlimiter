package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxBlockMixin {
    private static final String STACK_UNLIMITER$ITEM_COUNT_KEY = "container.shulkerBox.itemCount";

    @Inject(method = "appendHoverText", at = @At("RETURN"))
    private void stackUnlimiter$showExactContainedCounts(
            ItemStack shulker,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag,
            CallbackInfo callback
    ) {
        ItemContainerContents contents = shulker.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY
        );
        Iterator<ItemStack> containedItems = contents.nonEmptyItems().iterator();

        for (int lineIndex = 0; lineIndex < tooltip.size() && containedItems.hasNext(); lineIndex++) {
            Component line = tooltip.get(lineIndex);
            if (!(line.getContents() instanceof TranslatableContents translation)
                    || !STACK_UNLIMITER$ITEM_COUNT_KEY.equals(translation.getKey())) {
                continue;
            }

            ItemStack contained = containedItems.next();
            long exactCount = StackCounts.get(contained);
            if (exactCount == contained.getCount()) {
                continue;
            }

            Object[] sourceArguments = translation.getArgs();
            Object[] arguments = Arrays.copyOf(sourceArguments, sourceArguments.length);
            if (arguments.length < 2) {
                continue;
            }
            arguments[1] = exactCount;

            MutableComponent replacement = translation.getFallback() == null
                    ? Component.translatable(translation.getKey(), arguments)
                    : Component.translatableWithFallback(
                            translation.getKey(),
                            translation.getFallback(),
                            arguments
                    );
            replacement.setStyle(line.getStyle());
            line.getSiblings().forEach(replacement::append);
            tooltip.set(lineIndex, replacement);
        }
    }
}
