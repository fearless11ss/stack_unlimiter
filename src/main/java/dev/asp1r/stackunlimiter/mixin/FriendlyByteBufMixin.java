package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import dev.asp1r.stackunlimiter.internal.LongCountNbt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FriendlyByteBuf.class)
public abstract class FriendlyByteBufMixin {
    /**
     * Keeps the vanilla packet shape but always includes a transient exact-count
     * payload in the existing item NBT field.
     *
     * @author Stack Unlimiter
     * @reason Vanilla serializes the count as one byte and conditionally omits NBT.
     */
    @Overwrite(remap = false)
    public FriendlyByteBuf writeItemStack(ItemStack stack, boolean limitedTag) {
        FriendlyByteBuf self = (FriendlyByteBuf) (Object) this;
        if (stack.isEmpty()) {
            self.writeBoolean(false);
            return self;
        }

        self.writeBoolean(true);
        Item item = stack.getItem();
        self.writeId(BuiltInRegistries.ITEM, item);
        int safeCount = dev.asp1r.stackunlimiter.internal.LongCountMath.safeSerializedCount(
                StackCounts.get(stack),
                StackCounts.vanillaMaxStackSize(stack)
        );
        self.writeByte(safeCount);

        CompoundTag original = null;
        if (item.isDamageable(stack) || item.shouldOverrideMultiplayerNbt()) {
            original = limitedTag ? stack.getShareTag() : stack.getTag();
        }
        self.writeNbt(LongCountNbt.attachNetworkCount(original, StackCounts.get(stack)));
        return self;
    }

    /**
     * Reads and removes the transient exact-count payload before the item sees its
     * ordinary share tag.
     *
     * @author Stack Unlimiter
     * @reason Must mirror the exact-count metadata written by writeItemStack.
     */
    @Overwrite
    public ItemStack readItem() {
        FriendlyByteBuf self = (FriendlyByteBuf) (Object) this;
        if (!self.readBoolean()) {
            return ItemStack.EMPTY;
        }

        Item item = self.readById(BuiltInRegistries.ITEM);
        int safeCount = self.readByte();
        ItemStack result = new ItemStack(item, safeCount);
        CompoundTag networkTag = self.readNbt();
        Long exactCount = LongCountNbt.removeAndReadNetworkCount(networkTag);
        result.readShareTag(networkTag == null || networkTag.isEmpty() ? null : networkTag);
        if (exactCount == null) {
            throw new IllegalArgumentException("Missing Stack Unlimiter network count");
        }
        StackCounts.set(result, exactCount);
        return result;
    }
}
