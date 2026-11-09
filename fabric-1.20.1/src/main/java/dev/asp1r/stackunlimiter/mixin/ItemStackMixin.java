package dev.asp1r.stackunlimiter.mixin;

import dev.asp1r.stackunlimiter.api.StackCounts;
import dev.asp1r.stackunlimiter.internal.LongCountAccess;
import dev.asp1r.stackunlimiter.internal.LongCountMath;
import dev.asp1r.stackunlimiter.internal.LongCountNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements LongCountAccess {
    @Shadow
    private int count;

    @Shadow
    public abstract ItemStack copy();

    @Unique
    private long stackUnlimiter$exactCount;

    @Unique
    private boolean stackUnlimiter$initialized;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void stackUnlimiter$initialize(CallbackInfo callback) {
        this.stackUnlimiter$exactCount = Math.max(0, this.count);
        this.stackUnlimiter$initialized = true;
    }

    @Inject(method = "of", at = @At("RETURN"))
    private static void stackUnlimiter$load(CompoundTag tag, CallbackInfoReturnable<ItemStack> callback) {
        ItemStack result = callback.getReturnValue();
        Long exact = LongCountNbt.readPersistent(tag);
        if (exact != null && result != ItemStack.EMPTY) {
            ((LongCountAccess) (Object) result).stackUnlimiter$setLongCountUnchecked(exact);
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void stackUnlimiter$save(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback) {
        ItemStack self = (ItemStack) (Object) this;
        LongCountNbt.writePersistent(
                callback.getReturnValue(),
                this.stackUnlimiter$getLongCount(),
                StackCounts.vanillaMaxStackSize(self)
        );
    }

    @Inject(method = "getCount", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$getFacadeCount(CallbackInfoReturnable<Integer> callback) {
        if (this.stackUnlimiter$initialized) {
            callback.setReturnValue(this.count);
        }
    }

    @Inject(method = "setCount", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$setFacadeCount(int requested, CallbackInfo callback) {
        if (!this.stackUnlimiter$initialized) {
            return;
        }

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

    @Inject(method = "copy", at = @At("RETURN"))
    private void stackUnlimiter$copyExact(CallbackInfoReturnable<ItemStack> callback) {
        ItemStack result = callback.getReturnValue();
        if (result != ItemStack.EMPTY) {
            ((LongCountAccess) (Object) result).stackUnlimiter$setLongCountUnchecked(
                    this.stackUnlimiter$getLongCount()
            );
        }
    }

    @Inject(method = "copyWithCount", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$copyWithExactCount(int requested, CallbackInfoReturnable<ItemStack> callback) {
        callback.setReturnValue(StackCounts.copyWithCount((ItemStack) (Object) this, requested));
    }

    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void stackUnlimiter$getUnlimitedMaximum(CallbackInfoReturnable<Integer> callback) {
        ItemStack self = (ItemStack) (Object) this;
        if (StackCounts.isEligible(self)) {
            callback.setReturnValue(Integer.MAX_VALUE);
        }
    }

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private static void stackUnlimiter$matchesExact(
            ItemStack first,
            ItemStack second,
            CallbackInfoReturnable<Boolean> callback
    ) {
        callback.setReturnValue(first == second || (
                StackCounts.get(first) == StackCounts.get(second)
                        && ItemStack.isSameItemSameTags(first, second)
        ));
    }

    @Override
    public long stackUnlimiter$getLongCount() {
        return this.stackUnlimiter$initialized ? this.stackUnlimiter$exactCount : Math.max(0, this.count);
    }

    @Override
    public void stackUnlimiter$setLongCountUnchecked(long exactCount) {
        long validCount = LongCountMath.requireValid(exactCount);
        ItemStack self = (ItemStack) (Object) this;
        this.stackUnlimiter$exactCount = validCount;
        this.count = validCount == 0
                ? 0
                : (int) Math.min(validCount, StackCounts.vanillaMaxStackSize(self));
        this.stackUnlimiter$initialized = true;
    }
}
