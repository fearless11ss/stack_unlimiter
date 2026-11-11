package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(value = ItemContainerContents.class, remap = false)
public abstract class ItemContainerContentsMixin {
    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$showExactContainedCounts(
            Item.TooltipContext context,
            Consumer<Component> consumer,
            TooltipFlag flag,
            DataComponentGetter components,
            CallbackInfo callback
    ) {
        ItemContainerContents contents = (ItemContainerContents) (Object) this;
        List<ItemStack> containedItems = contents.nonEmptyItemCopyStream().toList();
        int lineCount = 0;

        for (ItemStack contained : containedItems) {
            if (lineCount <= 4) {
                lineCount++;
                consumer.accept(Component.translatable(
                        "item.container.item_count",
                        StackCounts.get(contained),
                        contained.getHoverName()
                ));
            }
        }

        int hiddenItems = containedItems.size() - lineCount;
        if (hiddenItems > 0) {
            consumer.accept(Component.translatable("item.container.more_items", hiddenItems)
                    .withStyle(ChatFormatting.ITALIC));
        }
        callback.cancel();
    }
}
