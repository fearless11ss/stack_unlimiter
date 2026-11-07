package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContainerMenu.class, remap = false)
public abstract class AbstractContainerMenuInteractionMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$dropExactStackOnce(
            int slotIndex,
            int buttonNum,
            ContainerInput containerInput,
            Player player,
            CallbackInfo callback
    ) {
        if (containerInput != ContainerInput.THROW || buttonNum != 1 || slotIndex < 0) {
            return;
        }

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (!menu.getCarried().isEmpty() || !menu.isValidSlotIndex(slotIndex)) {
            return;
        }

        Slot slot = menu.getSlot(slotIndex);
        ItemStack source = slot.getItem();
        long exactCount = StackCounts.get(source);
        if (source.isEmpty() || exactCount <= source.getCount() || !slot.mayPickup(player)) {
            return;
        }
        if (!player.canDropItems()) {
            callback.cancel();
            return;
        }

        ItemStack dropped = StackCounts.split(source, exactCount);
        slot.setByPlayer(ItemStack.EMPTY, dropped);
        slot.onTake(player, dropped);
        player.drop(dropped, true, Prediction.PREDICTED);
        player.handleCreativeModeItemDrop(dropped);
        callback.cancel();
    }
}
