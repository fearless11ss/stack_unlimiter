package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import dev.asp1r.stackunlimiter.client.StackCountDisplay;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiGraphicsExtractor.class, remap = false)
public abstract class GuiGraphicsMixin {
    @Inject(
            method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stackUnlimiter$renderCompactCount(
            Font font,
            ItemStack stack,
            int x,
            int y,
            String countText,
            CallbackInfo callback
    ) {
        if (countText != null || stack.isEmpty()) {
            return;
        }

        long exactCount = StackCounts.get(stack);
        if (!StackCountDisplay.requiresExactLabel(exactCount, stack.getCount())) {
            return;
        }

        GuiGraphicsExtractor graphics = (GuiGraphicsExtractor) (Object) this;
        graphics.itemDecorations(font, stack, x, y, "");

        String label = StackCountDisplay.format(exactCount);
        int width = font.width(label);
        float scale = StackCountDisplay.scaleForWidth(width);

        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 17.0F, y + 9.0F);
        graphics.pose().scale(scale, scale);
        graphics.text(font, label, -width, 0, StackCountDisplay.TEXT_COLOR, true);
        graphics.pose().popMatrix();
        callback.cancel();
    }
}
