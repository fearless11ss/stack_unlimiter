package dev.asp1r.stackunlimiter.gametest;

import com.mojang.authlib.GameProfile;
import dev.asp1r.stackunlimiter.StackUnlimiter;
import dev.asp1r.stackunlimiter.api.StackCounts;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

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

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void giveCommandUsesOneEligibleStack(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "stack-unlimiter-test")
        );
        player.getInventory().clearContent();

        try {
            helper.getLevel().getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack().withPermission(4),
                    "give @s minecraft:cobblestone 2000"
            );
        } catch (NullPointerException exception) {
            // The Forge mock player has no Netty channel. GiveCommand reaches that
            // harness limitation only after it has inserted the stack we assert below.
            if (exception.getMessage() == null
                    || (!exception.getMessage().contains("channel")
                    && !exception.getMessage().contains("connection"))) {
                throw exception;
            }
        }

        long total = 0;
        int occupiedSlots = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                occupiedSlots++;
                total += StackCounts.get(stack);
            }
        }
        assertEquals(1, occupiedSlots, "occupied inventory slots");
        assertEquals(2_000, total, "given item total");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void oversizedIngredientsStillMatchCraftingRecipes(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "stack-unlimiter-crafting-test")
        );
        InventoryMenu menu = player.inventoryMenu;
        CraftingContainer grid = menu.getCraftSlots();

        stackUnlimiter$setMockCraftingSlot(grid, 0, new ItemStack(Items.OAK_PLANKS, 96));
        stackUnlimiter$setMockCraftingSlot(grid, 2, new ItemStack(Items.OAK_PLANKS, 96));

        ItemStack result = menu.getSlot(menu.getResultSlotIndex()).getItem();
        if (!result.is(Items.STICK)) {
            throw new GameTestAssertException(
                    "oversized plank stacks should produce sticks, got " + result
            );
        }
        assertEquals(4, StackCounts.get(result), "crafted stick count");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void repeatedRecipePlacementDoesNotScatterIngredients(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "stack-unlimiter-recipe-book-test")
        );
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        player.connection = new ServerGamePacketListenerImpl(
                helper.getLevel().getServer(),
                connection,
                player
        );
        connection.setListener(player.connection);

        player.getInventory().clearContent();
        helper.getLevel().getServer().getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(4),
                "give @s minecraft:oak_planks 192000"
        );
        Recipe<?> craftingTable = helper.getLevel().getRecipeManager()
                .byKey(new ResourceLocation("minecraft", "crafting_table"))
                .orElseThrow(() -> new GameTestAssertException("crafting table recipe is missing"));
        ServerPlayer clientMirror = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "stack-unlimiter-client-mirror")
        );
        Connection mirrorConnection = new Connection(PacketFlow.SERVERBOUND);
        clientMirror.connection = new ServerGamePacketListenerImpl(
                helper.getLevel().getServer(),
                mirrorConnection,
                clientMirror
        );
        mirrorConnection.setListener(clientMirror.connection);

        long before = stackUnlimiter$countPlanks(player);
        for (int click = 1; click <= 20; click++) {
            player.inventoryMenu.handlePlacement(false, craftingTable, player);
            assertEquals(
                    before,
                    stackUnlimiter$countPlanks(player),
                    "planks after repeated recipe placement " + click
            );
            int inventoryPlankSlots = stackUnlimiter$countInventoryPlankSlots(player);
            if (inventoryPlankSlots > 1) {
                throw new GameTestAssertException(
                        "inventory plank slots after repeated recipe placement " + click
                                + ": expected at most 1, got " + inventoryPlankSlots
                );
            }
            ClientboundContainerSetContentPacket decoded = stackUnlimiter$roundTripMenu(
                    player.inventoryMenu
            );
            clientMirror.inventoryMenu.initializeContents(
                    decoded.getStateId(),
                    decoded.getItems(),
                    decoded.getCarriedItem()
            );
            assertEquals(
                    before,
                    stackUnlimiter$countPlanks(clientMirror),
                    "client planks after repeated recipe placement " + click
            );
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void shiftRecipePlacementUsesAllAvailableIngredients(GameTestHelper helper) {
        Recipe<?> craftingTable = helper.getLevel().getRecipeManager()
                .byKey(new ResourceLocation("minecraft", "crafting_table"))
                .orElseThrow(() -> new GameTestAssertException("crafting table recipe is missing"));
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        StackCounts.set(planks, 192_000);
        StackedContents contents = new StackedContents();
        contents.accountSimpleStack(planks);

        assertEquals(
                48_000,
                contents.getBiggestCraftableStack(craftingTable, null),
                "crafting-table recipes visible to shift placement"
        );

        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "stack-unlimiter-shift-recipe-test")
        );
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        player.connection = new ServerGamePacketListenerImpl(
                helper.getLevel().getServer(),
                connection,
                player
        );
        connection.setListener(player.connection);
        player.getInventory().clearContent();
        ItemStack inventoryPlanks = new ItemStack(Items.OAK_PLANKS);
        StackCounts.set(inventoryPlanks, 192_000);
        player.getInventory().setItem(0, inventoryPlanks);
        player.getRecipeBook().addRecipes(List.of(craftingTable), player);

        player.inventoryMenu.handlePlacement(true, craftingTable, player);

        for (int slot = 0; slot < 4; slot++) {
            assertEquals(
                    48_000,
                    StackCounts.get(player.inventoryMenu.getCraftSlots().getItem(slot)),
                    "shift-placed plank count in crafting slot " + slot
            );
        }
        assertEquals(0, stackUnlimiter$countInventoryPlankSlots(player), "remaining inventory plank slots");
        assertEquals(192_000, stackUnlimiter$countPlanks(player), "shift-placed plank total");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void shiftCraftingResultCraftsAllAvailableItems(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "stack-unlimiter-shift-result-test")
        );
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        player.connection = new ServerGamePacketListenerImpl(
                helper.getLevel().getServer(),
                connection,
                player
        );
        connection.setListener(player.connection);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.CRAFTING_TABLE, 10));

        CraftingContainer grid = player.inventoryMenu.getCraftSlots();
        for (int slot = 0; slot < 4; slot++) {
            ItemStack planks = new ItemStack(Items.OAK_PLANKS);
            StackCounts.set(planks, 250_000);
            grid.setItem(slot, planks);
        }

        long startedAt = System.nanoTime();
        player.inventoryMenu.clicked(
                player.inventoryMenu.getResultSlotIndex(),
                0,
                ClickType.QUICK_MOVE,
                player
        );
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        if (elapsedMillis > 5_000) {
            throw new GameTestAssertException(
                    "bulk shift-crafting took too long: " + elapsedMillis + " ms"
            );
        }

        assertEquals(
                250_010,
                stackUnlimiter$countInventoryItem(player, Items.CRAFTING_TABLE),
                "crafting tables collected by shift-click"
        );
        assertEquals(0, stackUnlimiter$countPlanks(player), "remaining crafting planks");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void returnedIngredientsMergeIntoAnOversizedStack(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "stack-unlimiter-ingredient-return-test")
        );
        player.getInventory().clearContent();

        ItemStack existing = new ItemStack(Items.OAK_PLANKS);
        StackCounts.set(existing, 192_000);
        player.getInventory().setItem(0, existing);
        player.getInventory().placeItemBackInInventory(
                new ItemStack(Items.OAK_PLANKS, 4),
                false
        );

        assertEquals(1, stackUnlimiter$countInventoryPlankSlots(player), "merged plank slots");
        assertEquals(192_004, stackUnlimiter$countPlanks(player), "merged plank total");
        helper.succeed();
    }

    private static ClientboundContainerSetContentPacket stackUnlimiter$roundTripMenu(
            InventoryMenu menu
    ) {
        ClientboundContainerSetContentPacket outgoing = new ClientboundContainerSetContentPacket(
                menu.containerId,
                menu.getStateId(),
                menu.getItems(),
                menu.getCarried()
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        outgoing.write(buffer);
        return new ClientboundContainerSetContentPacket(buffer);
    }

    private static int stackUnlimiter$countInventoryPlankSlots(ServerPlayer player) {
        int occupied = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(Items.OAK_PLANKS)) {
                occupied++;
            }
        }
        return occupied;
    }

    private static long stackUnlimiter$countInventoryItem(
            ServerPlayer player,
            net.minecraft.world.item.Item item
    ) {
        long total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += StackCounts.get(stack);
            }
        }
        return total;
    }

    private static long stackUnlimiter$countPlanks(ServerPlayer player) {
        long total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Items.OAK_PLANKS)) {
                total += StackCounts.get(stack);
            }
        }
        for (ItemStack stack : player.inventoryMenu.getCraftSlots().getItems()) {
            if (stack.is(Items.OAK_PLANKS)) {
                total += StackCounts.get(stack);
            }
        }
        return total;
    }

    private static void stackUnlimiter$setMockCraftingSlot(
            CraftingContainer grid,
            int slot,
            ItemStack stack
    ) {
        try {
            grid.setItem(slot, stack);
        } catch (NullPointerException exception) {
            // The result is computed before the mock player's missing connection
            // prevents the normal result-slot packet from being sent.
            if (exception.getMessage() == null || !exception.getMessage().contains("connection")) {
                throw exception;
            }
        }
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
