package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(value = AbstractContainerMenu.class, remap = false)
public abstract class AbstractContainerMenuInteractionMixin {
    @Unique
    private boolean stackUnlimiter$exactQuickCraftActive;

    @Unique
    private int stackUnlimiter$exactQuickCraftType;

    @Unique
    private final Set<Integer> stackUnlimiter$exactQuickCraftSlots = new LinkedHashSet<>();

    @Inject(method = "getQuickCraftPlaceCount", at = @At("HEAD"), cancellable = true)
    private static void stackUnlimiter$getExactQuickCraftPlaceCount(
            int slotCount,
            int quickCraftType,
            ItemStack carried,
            CallbackInfoReturnable<Integer> callback
    ) {
        long exactCount = StackCounts.get(carried);
        if (slotCount <= 0 || exactCount <= carried.getCount()) {
            return;
        }

        long placeCount = switch (quickCraftType) {
            case AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE -> exactCount / slotCount;
            case AbstractContainerMenu.QUICKCRAFT_TYPE_GREEDY -> 1;
            case AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE -> carried.getMaxStackSize();
            default -> exactCount;
        };
        callback.setReturnValue((int) Math.min(placeCount, Integer.MAX_VALUE));
    }

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$handleExactStackClick(
            int slotIndex,
            int buttonNum,
            ContainerInput containerInput,
            Player player,
            CallbackInfo callback
    ) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        if (containerInput == ContainerInput.QUICK_CRAFT) {
            if (stackUnlimiter$handleExactQuickCraft(menu, slotIndex, buttonNum, player)) {
                callback.cancel();
            }
            return;
        }
        stackUnlimiter$resetExactQuickCraft();

        if (containerInput == ContainerInput.PICKUP_ALL) {
            if (stackUnlimiter$pickupAllExactStacks(menu, slotIndex, buttonNum, player)) {
                callback.cancel();
            }
            return;
        }

        if (slotIndex < 0 || !menu.isValidSlotIndex(slotIndex)) {
            return;
        }

        Slot slot = menu.getSlot(slotIndex);
        ItemStack source = slot.getItem();
        ItemStack carried = menu.getCarried();

        if (containerInput == ContainerInput.PICKUP && (buttonNum == 0 || buttonNum == 1)) {
            if (carried.isEmpty()) {
                if (stackUnlimiter$pickupExactStack(menu, slot, source, buttonNum, player)) {
                    callback.cancel();
                }
            } else if (StackCounts.get(carried) > carried.getCount()
                    && stackUnlimiter$placeExactCarriedStack(menu, slot, source, carried, buttonNum, player)) {
                callback.cancel();
            }
            return;
        }

        if (!carried.isEmpty()
                || containerInput != ContainerInput.THROW
                || buttonNum != 1
                || source.isEmpty()
                || StackCounts.get(source) <= source.getCount()
                || !slot.mayPickup(player)) {
            return;
        }
        if (!player.canDropItems()) {
            callback.cancel();
            return;
        }

        ItemStack dropped = StackCounts.split(source, StackCounts.get(source));
        slot.setByPlayer(ItemStack.EMPTY, dropped);
        slot.onTake(player, dropped);
        player.drop(dropped, true, Prediction.PREDICTED);
        player.handleCreativeModeItemDrop(dropped);
        callback.cancel();
    }

    @Inject(method = "moveItemStackTo", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$moveExactStackTo(
            ItemStack source,
            int startSlot,
            int endSlot,
            boolean backwards,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (source.isEmpty()
                || !StackCounts.isEligible(source)
                || StackCounts.get(source) <= source.getCount()) {
            return;
        }

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        boolean changed = false;
        int index = backwards ? endSlot - 1 : startSlot;
        int step = backwards ? -1 : 1;

        while (!source.isEmpty() && index >= startSlot && index < endSlot) {
            Slot targetSlot = menu.getSlot(index);
            ItemStack target = targetSlot.getItem();
            if (!target.isEmpty() && StackCounts.sameItemSameComponents(source, target)) {
                long room = Long.MAX_VALUE - StackCounts.get(target);
                long moved = Math.min(room, StackCounts.get(source));
                if (moved > 0) {
                    StackCounts.shrink(source, moved);
                    StackCounts.grow(target, moved);
                    targetSlot.setChanged();
                    changed = true;
                }
            }
            index += step;
        }

        index = backwards ? endSlot - 1 : startSlot;
        while (!source.isEmpty() && index >= startSlot && index < endSlot) {
            Slot targetSlot = menu.getSlot(index);
            if (targetSlot.getItem().isEmpty() && targetSlot.mayPlace(source)) {
                ItemStack placed = StackCounts.split(source, StackCounts.get(source));
                targetSlot.setByPlayer(placed);
                targetSlot.setChanged();
                changed = true;
                break;
            }
            index += step;
        }

        callback.setReturnValue(changed);
    }

    @Unique
    private static boolean stackUnlimiter$pickupExactStack(
            AbstractContainerMenu menu,
            Slot slot,
            ItemStack source,
            int buttonNum,
            Player player
    ) {
        long exactCount = StackCounts.get(source);
        if (source.isEmpty() || exactCount <= source.getCount() || !slot.mayPickup(player)) {
            return false;
        }

        long requested = buttonNum == 0 ? exactCount : exactCount / 2 + exactCount % 2;
        ItemStack carried = StackCounts.split(source, requested);
        if (source.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, carried);
        } else {
            slot.setChanged();
        }
        menu.setCarried(carried);
        slot.onTake(player, carried);
        return true;
    }

    @Unique
    private static boolean stackUnlimiter$placeExactCarriedStack(
            AbstractContainerMenu menu,
            Slot slot,
            ItemStack source,
            ItemStack carried,
            int buttonNum,
            Player player
    ) {
        long requested = buttonNum == 0 ? StackCounts.get(carried) : 1;

        if (source.isEmpty()) {
            if (!slot.mayPlace(carried)) {
                return false;
            }
            slot.setByPlayer(StackCounts.split(carried, requested));
            slot.setChanged();
            stackUnlimiter$normalizeCarried(menu, carried);
            return true;
        }

        if (!slot.mayPickup(player)) {
            return false;
        }
        if (slot.mayPlace(carried) && StackCounts.sameItemSameComponents(source, carried)) {
            long moved = Math.min(requested, Long.MAX_VALUE - StackCounts.get(source));
            if (moved > 0) {
                StackCounts.shrink(carried, moved);
                StackCounts.grow(source, moved);
                slot.setChanged();
            }
            stackUnlimiter$normalizeCarried(menu, carried);
            return true;
        }
        if (!slot.mayPlace(carried)) {
            return false;
        }

        slot.setByPlayer(carried, source);
        menu.setCarried(source);
        slot.setChanged();
        return true;
    }

    @Unique
    private static boolean stackUnlimiter$pickupAllExactStacks(
            AbstractContainerMenu menu,
            int slotIndex,
            int buttonNum,
            Player player
    ) {
        ItemStack carried = menu.getCarried();
        if (slotIndex < 0
                || !menu.isValidSlotIndex(slotIndex)
                || carried.isEmpty()
                || !StackCounts.isEligible(carried)
                || !stackUnlimiter$hasExactPickupAllTarget(menu, carried, player)) {
            return false;
        }

        Slot clickedSlot = menu.getSlot(slotIndex);
        if (clickedSlot.hasItem() && clickedSlot.mayPickup(player)) {
            return false;
        }

        int index = buttonNum == 0 ? 0 : menu.slots.size() - 1;
        int step = buttonNum == 0 ? 1 : -1;
        while (index >= 0 && index < menu.slots.size() && StackCounts.get(carried) < Long.MAX_VALUE) {
            Slot targetSlot = menu.getSlot(index);
            ItemStack target = targetSlot.getItem();
            if (!target.isEmpty()
                    && targetSlot.mayPickup(player)
                    && menu.canTakeItemForPickAll(carried, targetSlot)
                    && StackCounts.sameItemSameComponents(target, carried)) {
                long movedCount = Math.min(StackCounts.get(target), Long.MAX_VALUE - StackCounts.get(carried));
                ItemStack moved = StackCounts.split(target, movedCount);
                StackCounts.grow(carried, movedCount);
                if (target.isEmpty()) {
                    targetSlot.setByPlayer(ItemStack.EMPTY, moved);
                } else {
                    targetSlot.setChanged();
                }
                targetSlot.onTake(player, moved);
            }
            index += step;
        }
        menu.setCarried(carried);
        return true;
    }

    @Unique
    private static boolean stackUnlimiter$hasExactPickupAllTarget(
            AbstractContainerMenu menu,
            ItemStack carried,
            Player player
    ) {
        for (Slot targetSlot : menu.slots) {
            ItemStack target = targetSlot.getItem();
            if (!target.isEmpty()
                    && StackCounts.get(target) > target.getCount()
                    && targetSlot.mayPickup(player)
                    && menu.canTakeItemForPickAll(carried, targetSlot)
                    && StackCounts.sameItemSameComponents(target, carried)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean stackUnlimiter$handleExactQuickCraft(
            AbstractContainerMenu menu,
            int slotIndex,
            int buttonNum,
            Player player
    ) {
        int header = AbstractContainerMenu.getQuickcraftHeader(buttonNum);
        int type = AbstractContainerMenu.getQuickcraftType(buttonNum);

        if (header == AbstractContainerMenu.QUICKCRAFT_HEADER_START) {
            ItemStack carried = menu.getCarried();
            if (carried.isEmpty()
                    || StackCounts.get(carried) <= carried.getCount()
                    || (type != AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE
                    && type != AbstractContainerMenu.QUICKCRAFT_TYPE_GREEDY)
                    || !AbstractContainerMenu.isValidQuickcraftType(type, player)) {
                stackUnlimiter$resetExactQuickCraft();
                return false;
            }
            stackUnlimiter$exactQuickCraftActive = true;
            stackUnlimiter$exactQuickCraftType = type;
            stackUnlimiter$exactQuickCraftSlots.clear();
            return true;
        }

        if (!stackUnlimiter$exactQuickCraftActive || type != stackUnlimiter$exactQuickCraftType) {
            stackUnlimiter$resetExactQuickCraft();
            return false;
        }

        if (header == AbstractContainerMenu.QUICKCRAFT_HEADER_CONTINUE) {
            if (slotIndex >= 0 && menu.isValidSlotIndex(slotIndex)) {
                Slot slot = menu.getSlot(slotIndex);
                ItemStack target = slot.getItem();
                ItemStack carried = menu.getCarried();
                if (slot.mayPlace(carried)
                        && menu.canDragTo(slot)
                        && (target.isEmpty() || StackCounts.sameItemSameComponents(target, carried))) {
                    stackUnlimiter$exactQuickCraftSlots.add(slotIndex);
                }
            }
            return true;
        }

        if (header != AbstractContainerMenu.QUICKCRAFT_HEADER_END) {
            stackUnlimiter$resetExactQuickCraft();
            return true;
        }

        ItemStack carried = menu.getCarried();
        int slotCount = stackUnlimiter$exactQuickCraftSlots.size();
        if (!carried.isEmpty() && slotCount > 0) {
            long amountPerSlot = stackUnlimiter$exactQuickCraftType
                    == AbstractContainerMenu.QUICKCRAFT_TYPE_GREEDY
                    ? 1
                    : StackCounts.get(carried) / slotCount;
            for (int targetIndex : stackUnlimiter$exactQuickCraftSlots) {
                if (amountPerSlot == 0 || carried.isEmpty()) {
                    break;
                }
                Slot targetSlot = menu.getSlot(targetIndex);
                ItemStack target = targetSlot.getItem();
                if (!targetSlot.mayPlace(carried)
                        || !menu.canDragTo(targetSlot)
                        || (!target.isEmpty() && !StackCounts.sameItemSameComponents(target, carried))) {
                    continue;
                }

                long moved = Math.min(amountPerSlot, StackCounts.get(carried));
                if (target.isEmpty()) {
                    targetSlot.setByPlayer(StackCounts.split(carried, moved));
                } else {
                    StackCounts.shrink(carried, moved);
                    StackCounts.grow(target, moved);
                }
                targetSlot.setChanged();
            }
        }
        stackUnlimiter$normalizeCarried(menu, carried);
        stackUnlimiter$resetExactQuickCraft();
        return true;
    }

    @Unique
    private static void stackUnlimiter$normalizeCarried(AbstractContainerMenu menu, ItemStack carried) {
        menu.setCarried(StackCounts.get(carried) == 0 ? ItemStack.EMPTY : carried);
    }

    @Unique
    private void stackUnlimiter$resetExactQuickCraft() {
        stackUnlimiter$exactQuickCraftActive = false;
        stackUnlimiter$exactQuickCraftType = -1;
        stackUnlimiter$exactQuickCraftSlots.clear();
    }
}


