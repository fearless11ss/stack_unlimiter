package dev.asp1r.stackunlimiter.gametest;

import dev.asp1r.stackunlimiter.StackUnlimiter;
import dev.asp1r.stackunlimiter.api.StackCounts;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(StackUnlimiter.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShulkerTooltipRegressionGameTests {
    private ShulkerTooltipRegressionGameTests() {
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void shulkerTooltipUsesExactContainedCount(GameTestHelper helper) {
        ItemStack planks = new ItemStack(Items.OAK_PLANKS, 64);
        StackCounts.set(planks, 400_000L);

        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        contents.set(0, planks);
        CompoundTag blockEntityData = ContainerHelper.saveAllItems(new CompoundTag(), contents);

        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        BlockItem.setBlockEntityData(shulker, BlockEntityType.SHULKER_BOX, blockEntityData);

        NonNullList<ItemStack> loaded = NonNullList.withSize(27, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(BlockItem.getBlockEntityData(shulker), loaded);
        assertEquals(400_000L, StackCounts.get(loaded.get(0)),
                "exact count stored inside shulker");

        List<Component> tooltip = new ArrayList<>();
        shulker.getItem().appendHoverText(
                shulker,
                helper.getLevel(),
                tooltip,
                TooltipFlag.NORMAL
        );
        String rendered = tooltip.stream()
                .map(Component::getString)
                .reduce((first, second) -> first + " | " + second)
                .orElse("");
        if (!rendered.contains("Oak Planks x400000")) {
            throw new GameTestAssertException(
                    "expected exact shulker tooltip count, got: " + rendered
            );
        }
        helper.succeed();
    }

    private static void assertEquals(long expected, long actual, String subject) {
        if (expected != actual) {
            throw new GameTestAssertException(
                    subject + ": expected " + expected + ", got " + actual
            );
        }
    }
}
