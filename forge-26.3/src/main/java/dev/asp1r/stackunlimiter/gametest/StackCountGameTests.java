package dev.asp1r.stackunlimiter.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.DynamicOps;
import dev.asp1r.stackunlimiter.StackUnlimiter;
import dev.asp1r.stackunlimiter.api.StackCounts;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.Connection;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestDontPrefix;
import net.minecraftforge.gametest.GameTestNamespace;

import java.util.UUID;

@GameTestNamespace(StackUnlimiter.MOD_ID)
@GameTestDontPrefix
public final class StackCountGameTests {
    private StackCountGameTests() {
    }

    @GameTest(name = "exact_count_survives_persistence_and_network")
    public static void exactCountSurvivesPersistenceAndNetwork(GameTestHelper helper) {
        long initial = (long) Integer.MAX_VALUE + 10_000L;
        ItemStack stack = new ItemStack(Items.COBBLESTONE, 64);
        StackCounts.set(stack, initial);

        assertEquals(initial, StackCounts.get(stack), "exact count after set");
        assertEquals(64, stack.getCount(), "serialization-safe vanilla count");
        assertEquals(Integer.MAX_VALUE, stack.getMaxStackSize(), "raised item maximum");

        stack.grow(25);
        stack.shrink(5);
        assertEquals(initial + 20, StackCounts.get(stack), "relative operations");

        ItemStack split = stack.split(17);
        assertEquals(17, StackCounts.get(split), "split result");
        assertEquals(initial + 3, StackCounts.get(stack), "split remainder");

        DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
        Tag encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        ItemStack loaded = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
        assertEquals(StackCounts.get(stack), StackCounts.get(loaded), "persistent component round trip");

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                helper.getLevel().registryAccess()
        );
        try {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
            ItemStack decoded = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            assertEquals(StackCounts.get(stack), StackCounts.get(decoded), "network component round trip");
        } finally {
            buffer.release();
        }

        helper.succeed();
    }

    @GameTest(name = "intentionally_non_stackable_items_remain_non_stackable")
    public static void intentionallyNonStackableItemsRemainNonStackable(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        try {
            StackCounts.set(sword, 2);
            throw new GameTestAssertException(Component.literal("non-stackable item accepted a count of two"), 0);
        } catch (IllegalArgumentException expected) {
            assertEquals(1, sword.getMaxStackSize(), "non-stackable maximum");
        }
        helper.succeed();
    }

    @GameTest(name = "inventory_merges_oversized_stacks")
    public static void inventoryMergesOversizedStacks(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-inventory-test");
        player.getInventory().clearContent();

        ItemStack existing = new ItemStack(Items.OAK_PLANKS);
        StackCounts.set(existing, 192_000);
        player.getInventory().setItem(0, existing);
        player.getInventory().placeItemBackInInventory(
                new ItemStack(Items.OAK_PLANKS, 4),
                false,
                Prediction.SERVER_ONLY
        );

        assertEquals(1, countOccupiedInventorySlots(player), "occupied inventory slots");
        assertEquals(192_004, countInventoryItem(player, Items.OAK_PLANKS), "merged plank total");
        helper.succeed();
    }

    @GameTest(name = "give_command_uses_one_eligible_stack")
    public static void giveCommandUsesOneEligibleStack(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-give-test");
        player.getInventory().clearContent();

        try {
            helper.getLevel().getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.OWNER),
                    "give @s minecraft:cobblestone 2000"
            );
        } catch (NullPointerException exception) {
            if (exception.getMessage() == null
                    || (!exception.getMessage().contains("channel")
                    && !exception.getMessage().contains("connection"))) {
                throw exception;
            }
        }

        assertEquals(1, countOccupiedInventorySlots(player), "occupied inventory slots after give");
        assertEquals(2_000, countInventoryItem(player, Items.COBBLESTONE), "given item total");
        helper.succeed();
    }

    @GameTest(name = "drop_selected_item_removes_entire_oversized_stack")
    public static void dropSelectedItemRemovesEntireOversizedStack(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-drop-test");
        player.getInventory().clearContent();

        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        StackCounts.set(planks, 1_000_000);
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), planks);

        ItemStack removed = player.getInventory().removeFromSelected(true);

        assertEquals(1_000_000, StackCounts.get(removed), "fully dropped plank count");
        assertEquals(0, StackCounts.get(player.getInventory().getSelectedItem()), "remaining selected stack");
        helper.succeed();
    }

    @GameTest(name = "oversized_item_entity_pickup_preserves_exact_count")
    public static void oversizedItemEntityPickupPreservesExactCount(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-pickup-test");

        for (int attempt = 0; attempt < 100; attempt++) {
            player.getInventory().clearContent();
            ItemStack planks = new ItemStack(Items.OAK_PLANKS);
            StackCounts.set(planks, 1_000_000);
            ItemEntity dropped = new ItemEntity(helper.getLevel(), 0, 4, 0, planks);
            dropped.setNoPickUpDelay();

            dropped.playerTouch(player);

            assertEquals(
                    1_000_000,
                    countInventoryItem(player, Items.OAK_PLANKS),
                    "picked-up plank count on attempt " + attempt
            );
        }
        helper.succeed();
    }

    @GameTest(name = "container_click_packet_bulk_crafts_all_results")
    public static void containerClickPacketBulkCraftsAllResults(GameTestHelper helper) {
        ServerPlayer player = createConnectedPlayer(helper, "stack-unlimiter-packet-craft-test");
        player.getInventory().clearContent();

        CraftingContainer grid = player.inventoryMenu.getCraftSlots();
        for (int slot = 0; slot < 4; slot++) {
            ItemStack planks = new ItemStack(Items.OAK_PLANKS);
            StackCounts.set(planks, 250_000);
            grid.setItem(slot, planks);
        }

        player.connection.handleContainerClick(new ServerboundContainerClickPacket(
                player.inventoryMenu.containerId,
                player.inventoryMenu.getStateId(),
                (short) 0,
                (byte) 0,
                ContainerInput.QUICK_MOVE,
                Int2ObjectMaps.emptyMap(),
                HashedStack.EMPTY
        ));

        assertEquals(
                250_000,
                countInventoryItem(player, Items.CRAFTING_TABLE),
                "crafting tables collected through click packet"
        );
        for (int slot = 0; slot < 4; slot++) {
            assertEquals(0, StackCounts.get(grid.getItem(slot)), "remaining packet ingredient in slot " + slot);
        }
        helper.succeed();
    }

    @GameTest(name = "recipe_accounting_uses_the_exact_count")
    public static void recipeAccountingUsesTheExactCount(GameTestHelper helper) {
        RecipeHolder<?> craftingTable = craftingTableRecipe(helper);

        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        StackCounts.set(planks, 192_000);
        StackedItemContents contents = new StackedItemContents();
        contents.accountSimpleStack(planks);

        assertEquals(
                48_000,
                contents.getBiggestCraftableStack(craftingTable.value(), null),
                "crafting-table recipes visible to shift placement"
        );
        helper.succeed();
    }

    @GameTest(name = "shift_recipe_placement_uses_all_ingredients")
    public static void shiftRecipePlacementUsesAllIngredients(GameTestHelper helper) {
        ServerPlayer player = createConnectedPlayer(helper, "stack-unlimiter-placement-test");
        player.getInventory().clearContent();
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        StackCounts.set(planks, 192_000);
        player.getInventory().setItem(0, planks);

        player.inventoryMenu.handlePlacement(
                true,
                false,
                craftingTableRecipe(helper),
                helper.getLevel(),
                player.getInventory()
        );

        CraftingContainer grid = player.inventoryMenu.getCraftSlots();
        for (int slot = 0; slot < 4; slot++) {
            assertEquals(48_000, StackCounts.get(grid.getItem(slot)), "shift-placed count in slot " + slot);
        }
        assertEquals(0, countInventoryItem(player, Items.OAK_PLANKS), "remaining inventory planks");
        helper.succeed();
    }

    @GameTest(name = "shift_crafting_collects_all_results")
    public static void shiftCraftingCollectsAllResults(GameTestHelper helper) {
        ServerPlayer player = createConnectedPlayer(helper, "stack-unlimiter-shift-craft-test");
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.CRAFTING_TABLE, 10));

        CraftingContainer grid = player.inventoryMenu.getCraftSlots();
        for (int slot = 0; slot < 4; slot++) {
            ItemStack planks = new ItemStack(Items.OAK_PLANKS);
            StackCounts.set(planks, 250_000);
            grid.setItem(slot, planks);
        }

        player.inventoryMenu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);

        assertEquals(
                250_010,
                countInventoryItem(player, Items.CRAFTING_TABLE),
                "crafting tables collected by shift-click"
        );
        for (int slot = 0; slot < 4; slot++) {
            assertEquals(0, StackCounts.get(grid.getItem(slot)), "remaining ingredient in slot " + slot);
        }
        helper.succeed();
    }

    private static ServerPlayer createPlayer(GameTestHelper helper, String name) {
        return new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), name),
                ClientInformation.createDefault()
        );
    }

    private static ServerPlayer createConnectedPlayer(GameTestHelper helper, String name) {
        ServerPlayer player = createPlayer(helper, name);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new ServerGamePacketListenerImpl(
                helper.getLevel().getServer(),
                connection,
                player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false)
        );
        return player;
    }

    private static RecipeHolder<?> craftingTableRecipe(GameTestHelper helper) {
        ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(
                Registries.RECIPE,
                Identifier.parse("minecraft:crafting_table")
        );
        return helper.getLevel().recipeAccess()
                .byKey(recipeKey)
                .orElseThrow(() -> new GameTestAssertException(
                        Component.literal("crafting table recipe is missing"),
                        0
                ));
    }

    private static int countOccupiedInventorySlots(ServerPlayer player) {
        int occupied = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) {
                occupied++;
            }
        }
        return occupied;
    }

    private static long countInventoryItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        long total = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                total += StackCounts.get(stack);
            }
        }
        return total;
    }

    private static void assertEquals(long expected, long actual, String subject) {
        if (expected != actual) {
            throw new GameTestAssertException(
                    Component.literal(subject + ": expected " + expected + ", got " + actual),
                    0
            );
        }
    }
}
