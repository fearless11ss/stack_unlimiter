package dev.asp1r.stackunlimiter.gametest;

import dev.asp1r.stackunlimiter.StackUnlimiter;
import dev.asp1r.stackunlimiter.api.StackCounts;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(StackUnlimiter.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StackCountGameTests {
    private StackCountGameTests() {
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void exactCountRoundTrip(GameTestHelper helper) {
        long initial = (long) Integer.MAX_VALUE + 10_000L;
        ItemStack stack = new ItemStack(Items.COBBLESTONE, 64);
        StackCounts.set(stack, initial);

        assertEquals(initial, StackCounts.get(stack), "exact count after set");
        assertEquals(Integer.MAX_VALUE, stack.getCount(), "int compatibility facade");
        assertEquals(Integer.MAX_VALUE, stack.getMaxStackSize(), "raised item maximum");

        stack.grow(25);
        stack.shrink(5);
        assertEquals(initial + 20, StackCounts.get(stack), "relative operations");

        ItemStack split = stack.split(17);
        assertEquals(17, StackCounts.get(split), "split result");
        assertEquals(initial + 3, StackCounts.get(stack), "split remainder");

        CompoundTag saved = stack.save(new CompoundTag());
        ItemStack loaded = ItemStack.of(saved);
        assertEquals(StackCounts.get(stack), StackCounts.get(loaded), "NBT round trip");

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeItem(stack);
            ItemStack decoded = buffer.readItem();
            assertEquals(StackCounts.get(stack), StackCounts.get(decoded), "network round trip");
            assertEquals(stack.getTag(), decoded.getTag(), "network metadata cleanup");
        } finally {
            buffer.release();
        }

        helper.succeed();
    }

    private static void assertEquals(long expected, long actual, String subject) {
        if (expected != actual) {
            throw new GameTestAssertException(subject + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEquals(Object expected, Object actual, String subject) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new GameTestAssertException(subject + ": expected " + expected + ", got " + actual);
        }
    }
}
