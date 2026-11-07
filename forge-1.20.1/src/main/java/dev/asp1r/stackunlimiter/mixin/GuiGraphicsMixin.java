package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import dev.asp1r.stackunlimiter.client.StackCountDisplay;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Inject(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
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
        if (exactCount < 1_000) {
            return;
        }

        GuiGraphics graphics = (GuiGraphics) (Object) this;
        graphics.renderItemDecorations(font, stack, x, y, "");

        String label = StackCountDisplay.format(exactCount);
        int width = font.width(label);
        float scale = StackCountDisplay.scaleForWidth(width);

        graphics.pose().pushPose();
        graphics.pose().translate(x + 17.0F, y + 9.0F, 200.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, label, -width, 0, 0xFFFFFF, true);
        graphics.pose().popPose();
        callback.cancel();
    }
}
