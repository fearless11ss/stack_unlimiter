package dev.asp1r.stackunlimiter.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.DynamicOps;
import dev.asp1r.stackunlimiter.api.StackCounts;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.List;

public final class StackCountGameTests {
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

    public static void inventoryThrowDropsOneExactStack(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-menu-drop-test");
        player.getInventory().clearContent();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1))));

        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        StackCounts.set(planks, 100_000);
        player.getInventory().setItem(0, planks);

        player.inventoryMenu.clicked(36, 1, ContainerInput.THROW, player);

        assertEquals(0, countInventoryItem(player, Items.OAK_PLANKS), "remaining planks after menu throw");
        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                AABB.ofSize(player.position(), 8, 8, 8)
        );
        assertEquals(1, dropped.size(), "dropped item entity count");
        assertEquals(100_000, StackCounts.get(dropped.getFirst().getItem()), "dropped exact plank count");
        helper.succeed();
    }

    public static void inventoryShiftClickPreservesOversizedStack(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-menu-shift-test");
        player.getInventory().clearContent();

        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        StackCounts.set(planks, 100_000);
        player.getInventory().setItem(0, planks);

        player.inventoryMenu.clicked(36, 0, ContainerInput.QUICK_MOVE, player);

        assertEquals(100_000, countInventoryItem(player, Items.OAK_PLANKS), "plank total after shift-click");
        assertEquals(1, countOccupiedInventorySlots(player), "occupied slots after shift-click");
        helper.succeed();
    }

    public static void inventoryRightClickSplitsExactlyInHalf(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-menu-split-test");
        player.getInventory().clearContent();

        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        StackCounts.set(planks, 250_000);
        player.getInventory().setItem(0, planks);

        player.inventoryMenu.clicked(36, 1, ContainerInput.PICKUP, player);

        assertEquals(125_000, StackCounts.get(player.inventoryMenu.getCarried()), "right-click carried count");
        assertEquals(125_000, StackCounts.get(player.getInventory().getItem(0)), "right-click remaining count");
        helper.succeed();
    }

    public static void inventoryLeftClickPicksUpEntireExactStack(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-menu-pickup-test");
        player.getInventory().clearContent();

        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        StackCounts.set(planks, 250_000);
        player.getInventory().setItem(0, planks);

        player.inventoryMenu.clicked(36, 0, ContainerInput.PICKUP, player);

        assertEquals(250_000, StackCounts.get(player.inventoryMenu.getCarried()), "left-click carried count");
        assertEquals(0, StackCounts.get(player.getInventory().getItem(0)), "left-click remaining count");
        helper.succeed();
    }

    public static void inventoryLeftClickPlacesEntireExactCarriedStack(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-place-all-test");
        player.getInventory().clearContent();
        player.inventoryMenu.setCarried(exactStack(Items.OAK_PLANKS, 250_000));

        player.inventoryMenu.clicked(36, 0, ContainerInput.PICKUP, player);

        assertEquals(250_000, StackCounts.get(player.getInventory().getItem(0)), "left-click placed count");
        assertEquals(0, StackCounts.get(player.inventoryMenu.getCarried()), "left-click carried remainder");
        helper.succeed();
    }

    public static void inventoryRightClickPlacesOneFromExactCarriedStack(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-place-one-test");
        player.getInventory().clearContent();
        player.inventoryMenu.setCarried(exactStack(Items.OAK_PLANKS, 250_000));

        player.inventoryMenu.clicked(36, 1, ContainerInput.PICKUP, player);

        assertEquals(1, StackCounts.get(player.getInventory().getItem(0)), "right-click placed count");
        assertEquals(249_999, StackCounts.get(player.inventoryMenu.getCarried()), "right-click carried remainder");
        helper.succeed();
    }

    public static void inventoryLeftClickMergesEntireExactCarriedStack(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-merge-all-test");
        player.getInventory().clearContent();
        player.getInventory().setItem(0, exactStack(Items.OAK_PLANKS, 100_000));
        player.inventoryMenu.setCarried(exactStack(Items.OAK_PLANKS, 250_000));

        player.inventoryMenu.clicked(36, 0, ContainerInput.PICKUP, player);

        assertEquals(350_000, StackCounts.get(player.getInventory().getItem(0)), "left-click merged count");
        assertEquals(0, StackCounts.get(player.inventoryMenu.getCarried()), "left-click merge remainder");
        helper.succeed();
    }

    public static void inventoryRightClickMergesOneFromExactCarriedStack(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-merge-one-test");
        player.getInventory().clearContent();
        player.getInventory().setItem(0, exactStack(Items.OAK_PLANKS, 100_000));
        player.inventoryMenu.setCarried(exactStack(Items.OAK_PLANKS, 250_000));

        player.inventoryMenu.clicked(36, 1, ContainerInput.PICKUP, player);

        assertEquals(100_001, StackCounts.get(player.getInventory().getItem(0)), "right-click merged count");
        assertEquals(249_999, StackCounts.get(player.inventoryMenu.getCarried()), "right-click merge remainder");
        helper.succeed();
    }

    public static void inventoryLeftClickSwapsExactStacks(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-click-swap-test");
        player.getInventory().clearContent();
        player.getInventory().setItem(0, exactStack(Items.COBBLESTONE, 100_000));
        player.inventoryMenu.setCarried(exactStack(Items.OAK_PLANKS, 250_000));

        player.inventoryMenu.clicked(36, 0, ContainerInput.PICKUP, player);

        assertStack(Items.OAK_PLANKS, 250_000, player.getInventory().getItem(0), "left-click swapped slot");
        assertStack(Items.COBBLESTONE, 100_000, player.inventoryMenu.getCarried(), "left-click swapped cursor");
        helper.succeed();
    }

    public static void inventoryOutsideClicksDropExactCarriedAmounts(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-outside-drop-test");
        player.getInventory().clearContent();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1))));
        player.inventoryMenu.setCarried(exactStack(Items.OAK_PLANKS, 250_000));

        player.inventoryMenu.clicked(AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, 1, ContainerInput.PICKUP, player);
        assertEquals(249_999, StackCounts.get(player.inventoryMenu.getCarried()), "secondary outside-click remainder");
        player.inventoryMenu.clicked(AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, 0, ContainerInput.PICKUP, player);

        assertEquals(0, StackCounts.get(player.inventoryMenu.getCarried()), "primary outside-click remainder");
        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                AABB.ofSize(player.position(), 8, 8, 8)
        );
        assertEquals(2, dropped.size(), "outside-click entity count");
        assertEquals(250_000, dropped.stream().mapToLong(entity -> StackCounts.get(entity.getItem())).sum(),
                "outside-click dropped total");
        helper.succeed();
    }

    public static void inventoryNumberKeySwapsExactStacks(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-number-swap-test");
        player.getInventory().clearContent();
        player.getInventory().setItem(0, exactStack(Items.OAK_PLANKS, 250_000));
        player.getInventory().setItem(9, exactStack(Items.COBBLESTONE, 100_000));

        player.inventoryMenu.clicked(9, 0, ContainerInput.SWAP, player);

        assertStack(Items.COBBLESTONE, 100_000, player.getInventory().getItem(0), "number-key hotbar stack");
        assertStack(Items.OAK_PLANKS, 250_000, player.getInventory().getItem(9), "number-key inventory stack");
        helper.succeed();
    }

    public static void inventoryPickupAllCollectsAllExactStacks(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-pickup-all-test");
        player.getInventory().clearContent();
        player.getInventory().setItem(0, exactStack(Items.OAK_PLANKS, 100_000));
        player.getInventory().setItem(1, exactStack(Items.OAK_PLANKS, 150_000));
        player.inventoryMenu.setCarried(exactStack(Items.OAK_PLANKS, 50_000));

        player.inventoryMenu.clicked(38, 0, ContainerInput.PICKUP_ALL, player);

        assertEquals(300_000, StackCounts.get(player.inventoryMenu.getCarried()), "pickup-all carried total");
        assertEquals(0, countInventoryItem(player, Items.OAK_PLANKS), "pickup-all inventory remainder");
        helper.succeed();
    }

    public static void inventoryPickupAllFromSingleItemCollectsAllExactStacks(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-single-pickup-all-test");
        player.getInventory().clearContent();
        for (int slot = 9; slot < 18; slot++) {
            player.getInventory().setItem(slot, exactStack(Items.OAK_PLANKS, 1_111_111));
        }
        player.inventoryMenu.setCarried(exactStack(Items.OAK_PLANKS, 1));

        player.inventoryMenu.clicked(38, 0, ContainerInput.PICKUP_ALL, player);

        assertEquals(10_000_000, StackCounts.get(player.inventoryMenu.getCarried()),
                "pickup-all carried total from a single item");
        assertEquals(0, countInventoryItem(player, Items.OAK_PLANKS),
                "pickup-all inventory remainder from a single item");
        helper.succeed();
    }

    public static void inventoryPrimaryDragEvenlyDistributesExactStack(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-even-drag-test");
        player.getInventory().clearContent();
        player.inventoryMenu.setCarried(exactStack(Items.OAK_PLANKS, 250_001));

        dragAcross(player, 0, 36, 37);

        assertEquals(125_000, StackCounts.get(player.getInventory().getItem(0)), "primary drag first slot");
        assertEquals(125_000, StackCounts.get(player.getInventory().getItem(1)), "primary drag second slot");
        assertEquals(1, StackCounts.get(player.inventoryMenu.getCarried()), "primary drag remainder");
        helper.succeed();
    }

    public static void inventoryDragPreviewUsesExactStackCount(GameTestHelper helper) {
        ItemStack carried = exactStack(Items.OAK_PLANKS, 1_000_000);

        int previewCount = AbstractContainerMenu.getQuickCraftPlaceCount(
                18,
                AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE,
                carried
        );

        assertEquals(55_555, previewCount, "primary drag preview count");
        helper.succeed();
    }

    public static void inventorySecondaryDragPlacesOnePerSlot(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-one-drag-test");
        player.getInventory().clearContent();
        player.inventoryMenu.setCarried(exactStack(Items.OAK_PLANKS, 250_000));

        dragAcross(player, 1, 36, 37, 38);

        assertEquals(1, StackCounts.get(player.getInventory().getItem(0)), "secondary drag first slot");
        assertEquals(1, StackCounts.get(player.getInventory().getItem(1)), "secondary drag second slot");
        assertEquals(1, StackCounts.get(player.getInventory().getItem(2)), "secondary drag third slot");
        assertEquals(249_997, StackCounts.get(player.inventoryMenu.getCarried()), "secondary drag remainder");
        helper.succeed();
    }

    public static void inventoryThrowOnePreservesExactRemainder(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-throw-one-test");
        player.getInventory().clearContent();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1))));
        player.getInventory().setItem(0, exactStack(Items.OAK_PLANKS, 250_000));

        player.inventoryMenu.clicked(36, 0, ContainerInput.THROW, player);

        assertEquals(249_999, StackCounts.get(player.getInventory().getItem(0)), "throw-one remainder");
        assertEquals(1, helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                AABB.ofSize(player.position(), 8, 8, 8)
        ).stream().mapToLong(entity -> StackCounts.get(entity.getItem())).sum(), "throw-one dropped total");
        helper.succeed();
    }

    public static void inventoryMenuCloseReturnsExactCarriedStack(GameTestHelper helper) {
        ServerPlayer player = createConnectedPlayer(helper, "stack-unlimiter-close-menu-test");
        player.getInventory().clearContent();
        player.inventoryMenu.setCarried(exactStack(Items.OAK_PLANKS, 250_000));

        player.inventoryMenu.removed(player);

        assertEquals(250_000, countInventoryItem(player, Items.OAK_PLANKS), "menu-close inventory total");
        assertEquals(0, StackCounts.get(player.inventoryMenu.getCarried()), "menu-close carried remainder");
        assertEquals(1, countOccupiedInventorySlots(player), "menu-close occupied slots");
        helper.succeed();
    }

    public static void inventoryCloneUsesTheUnlimitedMaximum(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-clone-test");
        player.getInventory().clearContent();
        player.getAbilities().instabuild = true;
        player.getInventory().setItem(0, exactStack(Items.OAK_PLANKS, 250_000));

        player.inventoryMenu.clicked(36, 2, ContainerInput.CLONE, player);

        assertEquals(Integer.MAX_VALUE, StackCounts.get(player.inventoryMenu.getCarried()), "cloned carried count");
        assertEquals(250_000, StackCounts.get(player.getInventory().getItem(0)), "cloned source count");
        helper.succeed();
    }

    public static void chestShiftClickMovesExactStackToPlayer(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-chest-to-player-test");
        player.getInventory().clearContent();
        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(0, exactStack(Items.OAK_PLANKS, 250_000));
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), chest);

        menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);

        assertEquals(0, StackCounts.get(chest.getItem(0)), "chest source remainder");
        assertEquals(250_000, countInventoryItem(player, Items.OAK_PLANKS), "chest-to-player total");
        assertEquals(1, countOccupiedInventorySlots(player), "chest-to-player occupied slots");
        helper.succeed();
    }

    public static void chestShiftClickMovesExactStackFromPlayer(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-player-to-chest-test");
        player.getInventory().clearContent();
        player.getInventory().setItem(0, exactStack(Items.OAK_PLANKS, 250_000));
        SimpleContainer chest = new SimpleContainer(27);
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), chest);

        menu.clicked(54, 0, ContainerInput.QUICK_MOVE, player);

        assertEquals(0, countInventoryItem(player, Items.OAK_PLANKS), "player-to-chest inventory remainder");
        assertEquals(250_000, StackCounts.get(chest.getItem(0)), "player-to-chest total");
        helper.succeed();
    }

    public static void chestShiftClickMergesIntoExistingExactStack(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-chest-merge-test");
        player.getInventory().clearContent();
        player.getInventory().setItem(0, exactStack(Items.OAK_PLANKS, 100_000));
        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(0, exactStack(Items.OAK_PLANKS, 250_000));
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), chest);

        menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);

        assertEquals(0, StackCounts.get(chest.getItem(0)), "chest merge source remainder");
        assertEquals(350_000, countInventoryItem(player, Items.OAK_PLANKS), "chest merge player total");
        assertEquals(1, countOccupiedInventorySlots(player), "chest merge occupied slots");
        helper.succeed();
    }

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

    public static void itemEntityPickupGrowsExistingFullStack(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, "stack-unlimiter-existing-stack-pickup-test");
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.CHERRY_LOG, 64));

        ItemEntity dropped = new ItemEntity(
                helper.getLevel(),
                0,
                4,
                0,
                new ItemStack(Items.CHERRY_LOG, 1)
        );
        dropped.setNoPickUpDelay();

        dropped.playerTouch(player);

        assertEquals(65, countInventoryItem(player, Items.CHERRY_LOG), "merged cherry log count");
        if (!dropped.isRemoved()) {
            throw new GameTestAssertException(Component.literal("picked-up cherry log entity was not removed"), 0);
        }
        helper.succeed();
    }

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

    private static ItemStack exactStack(Item item, long count) {
        ItemStack stack = new ItemStack(item);
        StackCounts.set(stack, count);
        return stack;
    }

    private static void assertStack(Item item, long count, ItemStack stack, String subject) {
        if (!stack.is(item)) {
            throw new GameTestAssertException(
                    Component.literal(subject + ": expected " + item + ", got " + stack.getItem()),
                    0
            );
        }
        assertEquals(count, StackCounts.get(stack), subject + " count");
    }

    private static void dragAcross(ServerPlayer player, int type, int... slots) {
        player.inventoryMenu.clicked(
                AbstractContainerMenu.SLOT_CLICKED_OUTSIDE,
                AbstractContainerMenu.getQuickcraftMask(AbstractContainerMenu.QUICKCRAFT_HEADER_START, type),
                ContainerInput.QUICK_CRAFT,
                player
        );
        for (int slot : slots) {
            player.inventoryMenu.clicked(
                    slot,
                    AbstractContainerMenu.getQuickcraftMask(AbstractContainerMenu.QUICKCRAFT_HEADER_CONTINUE, type),
                    ContainerInput.QUICK_CRAFT,
                    player
            );
        }
        player.inventoryMenu.clicked(
                AbstractContainerMenu.SLOT_CLICKED_OUTSIDE,
                AbstractContainerMenu.getQuickcraftMask(AbstractContainerMenu.QUICKCRAFT_HEADER_END, type),
                ContainerInput.QUICK_CRAFT,
                player
        );
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
