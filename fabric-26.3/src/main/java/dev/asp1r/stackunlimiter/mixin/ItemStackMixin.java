package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import dev.asp1r.stackunlimiter.internal.LongCountAccess;
import dev.asp1r.stackunlimiter.internal.LongCountMath;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemStack.class, remap = false)
public abstract class ItemStackMixin implements LongCountAccess {
    @Shadow
    private int count;

    @Inject(method = "setCount", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$setFacadeCount(int requested, CallbackInfo callback) {
        if (requested < 0) {
            throw new IllegalArgumentException("Stack count cannot be negative: " + requested);
        }
        long current = this.stackUnlimiter$getLongCount();
        long updated = current > this.count && requested > 0
                ? LongCountMath.addExactNonNegative(current, (long) requested - this.count)
                : requested;
        this.stackUnlimiter$setLongCountUnchecked(updated);
        callback.cancel();
    }

    @Inject(method = "grow", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$growExact(int amount, CallbackInfo callback) {
        this.stackUnlimiter$setLongCountUnchecked(
                LongCountMath.addExactNonNegative(this.stackUnlimiter$getLongCount(), amount)
        );
        callback.cancel();
    }

    @Inject(method = "shrink", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$shrinkExact(int amount, CallbackInfo callback) {
        if (amount < 0) {
            throw new IllegalArgumentException("Shrink amount must be non-negative");
        }
        this.stackUnlimiter$setLongCountUnchecked(
                LongCountMath.subtractFloorZero(this.stackUnlimiter$getLongCount(), amount)
        );
        callback.cancel();
    }

    @Inject(method = "split", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$splitExact(int amount, CallbackInfoReturnable<ItemStack> callback) {
        callback.setReturnValue(StackCounts.split((ItemStack) (Object) this, amount));
    }

    @Inject(method = "copyWithCount", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$copyWithExactCount(int requested, CallbackInfoReturnable<ItemStack> callback) {
        callback.setReturnValue(StackCounts.copyWithCount((ItemStack) (Object) this, requested));
    }

    public int getMaxStackSize() {
        ItemStack self = (ItemStack) (Object) this;
        return StackCounts.isEligible(self)
                ? Integer.MAX_VALUE
                : StackCounts.vanillaMaxStackSize(self);
    }

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private static void stackUnlimiter$matchesExact(
            ItemStack first,
            ItemStack second,
            CallbackInfoReturnable<Boolean> callback
    ) {
        callback.setReturnValue(first == second || (
                StackCounts.get(first) == StackCounts.get(second)
                        && StackCounts.sameItemSameComponents(first, second)
        ));
    }

    @Inject(method = "isSameItemSameComponents", at = @At("HEAD"), cancellable = true)
    private static void stackUnlimiter$ignoreExactCountComponent(
            ItemStack first,
            ItemStack second,
            CallbackInfoReturnable<Boolean> callback
    ) {
        callback.setReturnValue(StackCounts.sameItemSameComponents(first, second));
    }

    @Override
    public long stackUnlimiter$getLongCount() {
        ItemStack self = (ItemStack) (Object) this;
        CustomData customData = self.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return Math.max(0, this.count);
        }
        return customData.copyTag()
                .getLong(LongCountAccess.EXACT_COUNT_KEY)
                .map(LongCountMath::requireValid)
                .orElse((long) Math.max(0, this.count));
    }

    @Override
    public void stackUnlimiter$setLongCountUnchecked(long exactCount) {
        long validCount = LongCountMath.requireValid(exactCount);
        ItemStack self = (ItemStack) (Object) this;
        int rawCount = validCount == 0
                ? 0
                : (int) Math.min(validCount, StackCounts.vanillaMaxStackSize(self));

        CustomData.update(DataComponents.CUSTOM_DATA, self, tag -> {
            if (validCount == rawCount) {
                tag.remove(LongCountAccess.EXACT_COUNT_KEY);
            } else {
                tag.putLong(LongCountAccess.EXACT_COUNT_KEY, validCount);
            }
        });
        this.count = rawCount;
    }
}
