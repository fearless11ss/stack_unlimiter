package dev.asp1r.stackunlimiter.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.DynamicOps;
import dev.asp1r.stackunlimiter.StackUnlimiter;
import dev.asp1r.stackunlimiter.api.StackCounts;
import io.netty.buffer.Unpooled;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.GameTestDontPrefix;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(value = StackUnlimiter.MOD_ID, namespace = StackUnlimiter.MOD_ID)
@GameTestDontPrefix
public final class StackCountGameTests {
    private StackCountGameTests() {
    }

    @GameTest(template = "empty")
    public static void exactCountRoundTrip(GameTestHelper helper) {
        long initial = (long) Integer.MAX_VALUE + 10_000L;
        ItemStack stack = new ItemStack(Items.COBBLESTONE, 64);
        StackCounts.set(stack, initial);

        assertEquals(initial, StackCounts.get(stack), "exact count after set");
        assertEquals(64, stack.getCount(), "vanilla compatibility facade");
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
        assertEquals(StackCounts.get(stack), StackCounts.get(loaded), "data-component round trip");

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                helper.getLevel().registryAccess()
        );
        try {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
            ItemStack decoded = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            assertEquals(StackCounts.get(stack), StackCounts.get(decoded), "network round trip");
        } finally {
            buffer.release();
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shulkerTooltipUsesExactContainedCount(GameTestHelper helper) {
        ItemStack planks = new ItemStack(Items.OAK_PLANKS, 64);
        StackCounts.set(planks, 400_000L);

        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(planks)));

        ItemStack stored = shulker.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItems()
                .iterator()
                .next();
        assertEquals(400_000L, StackCounts.get(stored), "exact count stored inside shulker");

        List<Component> tooltip = new ArrayList<>();
        shulker.getItem().appendHoverText(
                shulker,
                Item.TooltipContext.of(helper.getLevel()),
                tooltip,
                TooltipFlag.NORMAL
        );

        for (Component line : tooltip) {
            if (line.getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals("container.shulkerBox.itemCount")) {
                Object displayedCount = contents.getArgs()[1];
                assertEquals(400_000L, ((Number) displayedCount).longValue(),
                        "shulker tooltip contained count");
                helper.succeed();
                return;
            }
        }
        throw new GameTestAssertException("shulker tooltip item-count line was not generated");
    }

    @GameTest(template = "empty")
    public static void giveCommandUsesOneEligibleStack(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-test");
        player.getInventory().clearContent();

        stackUnlimiter$performCommand(helper, player, "give @s minecraft:cobblestone 2000");

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

    @GameTest(template = "empty")
    public static void giveCommandSynchronizesNonStackableItems(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-non-stackable-give-test");
        player.getInventory().clearContent();

        stackUnlimiter$performCommand(helper, player, "give @s minecraft:diamond_pickaxe");
        stackUnlimiter$performCommand(helper, player, "give @s minecraft:shulker_box");

        assertEquals(1, stackUnlimiter$countInventoryItem(player, Items.DIAMOND_PICKAXE),
                "server pickaxe count after give");
        assertEquals(1, stackUnlimiter$countInventoryItem(player, Items.SHULKER_BOX),
                "server shulker-box count after give");

        ClientboundContainerSetContentPacket decoded = stackUnlimiter$roundTripMenu(
                player.inventoryMenu,
                helper.getLevel().registryAccess()
        );
        long clientPickaxes = 0;
        long clientShulkerBoxes = 0;
        for (ItemStack stack : decoded.getItems()) {
            if (stack.is(Items.DIAMOND_PICKAXE)) {
                clientPickaxes += StackCounts.get(stack);
            } else if (stack.is(Items.SHULKER_BOX)) {
                clientShulkerBoxes += StackCounts.get(stack);
            }
        }
        assertEquals(1, clientPickaxes, "client pickaxe count after give synchronization");
        assertEquals(1, clientShulkerBoxes, "client shulker-box count after give synchronization");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void oversizedIngredientsStillMatchCraftingRecipes(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-crafting-test");
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

    @GameTest(template = "empty")
    public static void repeatedRecipePlacementDoesNotScatterIngredients(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-recipe-book-test");
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        player.connection = new ServerGamePacketListenerImpl(
                helper.getLevel().getServer(),
                connection,
                player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false)
        );

        player.getInventory().clearContent();
        helper.getLevel().getServer().getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(4),
                "give @s minecraft:oak_planks 192000"
        );
        RecipeHolder<?> craftingTable = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_table"))
                .orElseThrow(() -> new GameTestAssertException("crafting table recipe is missing"));
        ServerPlayer clientMirror = stackUnlimiter$createPlayer(helper, "stack-unlimiter-client-mirror");
        Connection mirrorConnection = new Connection(PacketFlow.SERVERBOUND);
        clientMirror.connection = new ServerGamePacketListenerImpl(
                helper.getLevel().getServer(),
                mirrorConnection,
                clientMirror,
                CommonListenerCookie.createInitial(clientMirror.getGameProfile(), false)
        );

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
                    player.inventoryMenu,
                    helper.getLevel().registryAccess()
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

    @GameTest(template = "empty")
    public static void shiftRecipePlacementUsesAllAvailableIngredients(GameTestHelper helper) {
        RecipeHolder<?> craftingTable = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_table"))
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

        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-shift-recipe-test");
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        player.connection = new ServerGamePacketListenerImpl(
                helper.getLevel().getServer(),
                connection,
                player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false)
        );
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

    @GameTest(template = "empty")
    public static void shiftCraftingResultCraftsAllAvailableItems(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-shift-result-test");
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        player.connection = new ServerGamePacketListenerImpl(
                helper.getLevel().getServer(),
                connection,
                player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false)
        );
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

    @GameTest(template = "empty")
    public static void returnedIngredientsMergeIntoAnOversizedStack(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-ingredient-return-test");
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

    @GameTest(template = "empty")
    public static void chestShiftClickMovesEntireOversizedStack(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-chest-transfer-test");
        player.getInventory().clearContent();
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        StackCounts.set(planks, 100_000);
        player.getInventory().setItem(0, planks);
        SimpleContainer chest = new SimpleContainer(27);
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), chest);

        menu.clicked(54, 0, ClickType.QUICK_MOVE, player);

        assertEquals(0, StackCounts.get(player.getInventory().getItem(0)),
                "player inventory remainder after chest Shift-click");
        assertEquals(100_000, StackCounts.get(chest.getItem(0)),
                "chest stack after player Shift-click");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryPrimaryAndSecondaryPickupUseExactCounts(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-pickup-test");
        player.getInventory().setItem(0, stackUnlimiter$exactStack(Items.OAK_PLANKS, 250_000));

        player.inventoryMenu.clicked(36, 1, ClickType.PICKUP, player);
        assertEquals(125_000, StackCounts.get(player.inventoryMenu.getCarried()), "right-click carried half");
        assertEquals(125_000, StackCounts.get(player.getInventory().getItem(0)), "right-click source half");

        player.inventoryMenu.setCarried(ItemStack.EMPTY);
        player.inventoryMenu.clicked(36, 0, ClickType.PICKUP, player);
        assertEquals(125_000, StackCounts.get(player.inventoryMenu.getCarried()), "left-click carried stack");
        assertEquals(0, StackCounts.get(player.getInventory().getItem(0)), "left-click source remainder");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryPlacesAndMergesExactCarriedStacks(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-place-merge-test");
        player.inventoryMenu.setCarried(stackUnlimiter$exactStack(Items.OAK_PLANKS, 250_000));

        player.inventoryMenu.clicked(36, 1, ClickType.PICKUP, player);
        assertEquals(1, StackCounts.get(player.getInventory().getItem(0)), "right-click placed count");
        assertEquals(249_999, StackCounts.get(player.inventoryMenu.getCarried()), "right-click carried remainder");

        player.inventoryMenu.clicked(36, 0, ClickType.PICKUP, player);
        assertEquals(250_000, StackCounts.get(player.getInventory().getItem(0)), "left-click merged count");
        assertEquals(0, StackCounts.get(player.inventoryMenu.getCarried()), "left-click carried remainder");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventorySwapsDifferentExactStacks(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-swap-test");
        player.getInventory().setItem(0, stackUnlimiter$exactStack(Items.COBBLESTONE, 100_000));
        player.inventoryMenu.setCarried(stackUnlimiter$exactStack(Items.OAK_PLANKS, 250_000));

        player.inventoryMenu.clicked(36, 0, ClickType.PICKUP, player);

        stackUnlimiter$assertStack(Items.OAK_PLANKS, 250_000, player.getInventory().getItem(0), "swapped slot");
        stackUnlimiter$assertStack(Items.COBBLESTONE, 100_000, player.inventoryMenu.getCarried(), "swapped carried");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryNumberKeySwapsExactStacks(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-number-swap-test");
        player.getInventory().setItem(0, stackUnlimiter$exactStack(Items.OAK_PLANKS, 250_000));
        player.getInventory().setItem(9, stackUnlimiter$exactStack(Items.COBBLESTONE, 100_000));

        player.inventoryMenu.clicked(9, 0, ClickType.SWAP, player);

        stackUnlimiter$assertStack(Items.COBBLESTONE, 100_000, player.getInventory().getItem(0), "number-key hotbar");
        stackUnlimiter$assertStack(Items.OAK_PLANKS, 250_000, player.getInventory().getItem(9), "number-key inventory");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryPickupAllCollectsExactStacks(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-pickup-all-test");
        for (int slot = 9; slot < 18; slot++) {
            player.getInventory().setItem(slot, stackUnlimiter$exactStack(Items.OAK_PLANKS, 1_111_111));
        }
        player.inventoryMenu.setCarried(stackUnlimiter$exactStack(Items.OAK_PLANKS, 1));

        player.inventoryMenu.clicked(38, 0, ClickType.PICKUP_ALL, player);

        assertEquals(10_000_000, StackCounts.get(player.inventoryMenu.getCarried()), "pickup-all carried total");
        assertEquals(0, stackUnlimiter$countInventoryItem(player, Items.OAK_PLANKS), "pickup-all remainder");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryPrimaryDragDistributesExactStack(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-primary-drag-test");
        player.inventoryMenu.setCarried(stackUnlimiter$exactStack(Items.OAK_PLANKS, 250_001));

        stackUnlimiter$dragAcross(player, 0, 36, 37);

        assertEquals(125_000, StackCounts.get(player.getInventory().getItem(0)), "primary drag first slot");
        assertEquals(125_000, StackCounts.get(player.getInventory().getItem(1)), "primary drag second slot");
        assertEquals(1, StackCounts.get(player.inventoryMenu.getCarried()), "primary drag remainder");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventorySecondaryDragPlacesOnePerSlot(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-secondary-drag-test");
        player.inventoryMenu.setCarried(stackUnlimiter$exactStack(Items.OAK_PLANKS, 250_000));

        stackUnlimiter$dragAcross(player, 1, 36, 37, 38);

        assertEquals(1, StackCounts.get(player.getInventory().getItem(0)), "secondary drag first slot");
        assertEquals(1, StackCounts.get(player.getInventory().getItem(1)), "secondary drag second slot");
        assertEquals(1, StackCounts.get(player.getInventory().getItem(2)), "secondary drag third slot");
        assertEquals(249_997, StackCounts.get(player.inventoryMenu.getCarried()), "secondary drag remainder");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestShiftClickMovesAndMergesExactStacksToPlayer(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-chest-player-test");
        player.getInventory().setItem(0, stackUnlimiter$exactStack(Items.OAK_PLANKS, 100_000));
        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(0, stackUnlimiter$exactStack(Items.OAK_PLANKS, 250_000));
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), chest);

        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);

        assertEquals(0, StackCounts.get(chest.getItem(0)), "chest source remainder");
        assertEquals(350_000, stackUnlimiter$countInventoryItem(player, Items.OAK_PLANKS), "merged player total");
        assertEquals(1, stackUnlimiter$countOccupiedInventorySlots(player), "merged player slots");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryOutsideClicksDropExactAmounts(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-outside-drop-test");
        player.inventoryMenu.setCarried(stackUnlimiter$exactStack(Items.OAK_PLANKS, 250_000));

        player.inventoryMenu.clicked(
                AbstractContainerMenu.SLOT_CLICKED_OUTSIDE,
                1,
                ClickType.PICKUP,
                player
        );
        assertEquals(249_999, StackCounts.get(player.inventoryMenu.getCarried()),
                "right outside-click carried remainder");

        player.inventoryMenu.clicked(
                AbstractContainerMenu.SLOT_CLICKED_OUTSIDE,
                0,
                ClickType.PICKUP,
                player
        );
        assertEquals(0, StackCounts.get(player.inventoryMenu.getCarried()),
                "left outside-click carried remainder");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryThrowKeyDropsOneThenEntireExactStack(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-throw-test");
        player.getInventory().setItem(0, stackUnlimiter$exactStack(Items.OAK_PLANKS, 250_000));

        player.inventoryMenu.clicked(36, 0, ClickType.THROW, player);
        assertEquals(249_999, StackCounts.get(player.getInventory().getItem(0)),
                "single-item throw remainder");

        player.inventoryMenu.clicked(36, 1, ClickType.THROW, player);
        assertEquals(0, StackCounts.get(player.getInventory().getItem(0)),
                "full-stack throw remainder");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hotbarFullStackThrowCreatesOneExactItemEntity(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-item-entity-drop-test");
        player.setPos(helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)).getCenter());
        player.getInventory().setItem(0, stackUnlimiter$exactStack(Items.OAK_PLANKS, 250_000));

        ItemStack removed = player.getInventory().removeFromSelected(true);
        assertEquals(250_000, StackCounts.get(removed), "hotbar full-stack removal count");
        player.drop(removed, false);

        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(player.blockPosition()).inflate(5),
                entity -> entity.getItem().is(Items.OAK_PLANKS)
        );
        assertEquals(1, drops.size(), "full-stack item entity count");
        assertEquals(250_000, StackCounts.get(drops.get(0).getItem()), "full-stack dropped item count");

        drops.get(0).setNoPickUpDelay();
        drops.get(0).playerTouch(player);
        assertEquals(250_000, stackUnlimiter$countInventoryItem(player, Items.OAK_PLANKS),
                "full-stack pickup round trip");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemEntityPickupMergesPastVanillaLimit(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-item-pickup-test");
        player.getInventory().setItem(0, new ItemStack(Items.OAK_LOG, 64));
        CapturingSynchronizer synchronizer = new CapturingSynchronizer();
        player.inventoryMenu.setSynchronizer(synchronizer);
        assertEquals(64, StackCounts.get(synchronizer.initialItems.get(36)),
                "initial synchronized log count");
        synchronizer.clearSlotChange();
        ItemEntity droppedLog = new ItemEntity(
                helper.getLevel(),
                player.getX(),
                player.getY(),
                player.getZ(),
                new ItemStack(Items.OAK_LOG)
        );
        droppedLog.setNoPickUpDelay();

        droppedLog.playerTouch(player);

        assertEquals(65, StackCounts.get(player.getInventory().getItem(0)),
                "log count after item-entity pickup");
        if (!droppedLog.isRemoved()) {
            throw new GameTestAssertException("picked-up item entity should be removed");
        }
        player.inventoryMenu.broadcastChanges();
        assertEquals(36, synchronizer.changedSlot, "synchronized inventory slot");
        assertEquals(65, StackCounts.get(synchronizer.changedStack),
                "broadcast log count after item-entity pickup");
        ClientboundContainerSetContentPacket decoded = stackUnlimiter$roundTripMenu(player.inventoryMenu, helper.getLevel().registryAccess());
        assertEquals(65, StackCounts.get(decoded.getItems().get(36)),
                "client log count after inventory synchronization");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void closingContainerReturnsExactCarriedStack(GameTestHelper helper) {
        ServerPlayer player = stackUnlimiter$createPlayer(helper, "stack-unlimiter-close-menu-test");
        SimpleContainer chest = new SimpleContainer(27);
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), chest);
        menu.setCarried(stackUnlimiter$exactStack(Items.OAK_PLANKS, 250_000));

        menu.removed(player);

        assertEquals(250_000, stackUnlimiter$countInventoryItem(player, Items.OAK_PLANKS),
                "returned carried total");
        assertEquals(0, StackCounts.get(menu.getCarried()), "carried stack after close");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void forgeItemHandlerPreservesExactInsertMergeAndExtract(GameTestHelper helper) {
        ItemStackHandler handler = new ItemStackHandler(1);
        ItemStack first = stackUnlimiter$exactStack(Items.OAK_PLANKS, 250_000);

        ItemStack simulatedRemainder = handler.insertItem(0, first, true);
        assertEquals(0, StackCounts.get(simulatedRemainder), "simulated insertion remainder");
        assertEquals(0, StackCounts.get(handler.getStackInSlot(0)), "simulated insertion contents");

        ItemStack remainder = handler.insertItem(0, first, false);
        assertEquals(0, StackCounts.get(remainder), "insertion remainder");
        assertEquals(250_000, StackCounts.get(handler.getStackInSlot(0)), "inserted contents");

        handler.insertItem(0, stackUnlimiter$exactStack(Items.OAK_PLANKS, 100_000), false);
        assertEquals(350_000, StackCounts.get(handler.getStackInSlot(0)), "merged contents");

        ItemStack simulatedExtract = handler.extractItem(0, 100_000, true);
        assertEquals(100_000, StackCounts.get(simulatedExtract), "simulated extraction result");
        assertEquals(350_000, StackCounts.get(handler.getStackInSlot(0)), "simulated extraction contents");

        ItemStack extracted = handler.extractItem(0, 100_000, false);
        assertEquals(100_000, StackCounts.get(extracted), "extraction result");
        assertEquals(250_000, StackCounts.get(handler.getStackInSlot(0)), "extraction remainder");

        ItemStackHandler loaded = new ItemStackHandler();
        loaded.deserializeNBT(
                helper.getLevel().registryAccess(),
                handler.serializeNBT(helper.getLevel().registryAccess())
        );
        assertEquals(250_000, StackCounts.get(loaded.getStackInSlot(0)), "handler NBT round trip");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nonStackableItemsRemainNonStackable(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        assertEquals(1, sword.getMaxStackSize(), "non-stackable item maximum");
        assertEquals(1, StackCounts.get(sword), "non-stackable exact count");

        boolean rejected = false;
        try {
            StackCounts.set(sword, 2);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        if (!rejected) {
            throw new GameTestAssertException("non-stackable item accepted an oversized count");
        }
        assertEquals(1, StackCounts.get(sword), "non-stackable count after rejection");
        helper.succeed();
    }

    private static ClientboundContainerSetContentPacket stackUnlimiter$roundTripMenu(
            InventoryMenu menu,
            RegistryAccess registryAccess
    ) {
        ClientboundContainerSetContentPacket outgoing = new ClientboundContainerSetContentPacket(
                menu.containerId,
                menu.getStateId(),
                menu.getItems(),
                menu.getCarried()
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        try {
            ClientboundContainerSetContentPacket.STREAM_CODEC.encode(buffer, outgoing);
            return ClientboundContainerSetContentPacket.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    private static void stackUnlimiter$performCommand(
            GameTestHelper helper,
            ServerPlayer player,
            String command
    ) {
        try {
            helper.getLevel().getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack().withPermission(4),
                    command
            );
        } catch (NullPointerException exception) {
            // The mock player has no Netty channel. GiveCommand reaches that
            // harness limitation only after it has inserted the stack asserted by the test.
            if (exception.getMessage() == null
                    || (!exception.getMessage().contains("channel")
                    && !exception.getMessage().contains("connection"))) {
                throw exception;
            }
        }
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

    private static ServerPlayer stackUnlimiter$createPlayer(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                profile,
                ClientInformation.createDefault()
        );
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        player.connection = new ServerGamePacketListenerImpl(
                helper.getLevel().getServer(),
                connection,
                player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false)
        );
        player.getInventory().clearContent();
        return player;
    }

    private static ItemStack stackUnlimiter$exactStack(net.minecraft.world.item.Item item, long count) {
        ItemStack stack = new ItemStack(item);
        StackCounts.set(stack, count);
        return stack;
    }

    private static void stackUnlimiter$assertStack(
            net.minecraft.world.item.Item item,
            long count,
            ItemStack stack,
            String subject
    ) {
        if (!stack.is(item)) {
            throw new GameTestAssertException(subject + ": expected " + item + ", got " + stack.getItem());
        }
        assertEquals(count, StackCounts.get(stack), subject + " count");
    }

    private static int stackUnlimiter$countOccupiedInventorySlots(ServerPlayer player) {
        int occupied = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                occupied++;
            }
        }
        return occupied;
    }

    private static void stackUnlimiter$dragAcross(ServerPlayer player, int type, int... slots) {
        player.inventoryMenu.clicked(
                AbstractContainerMenu.SLOT_CLICKED_OUTSIDE,
                AbstractContainerMenu.getQuickcraftMask(0, type),
                ClickType.QUICK_CRAFT,
                player
        );
        for (int slot : slots) {
            player.inventoryMenu.clicked(
                    slot,
                    AbstractContainerMenu.getQuickcraftMask(1, type),
                    ClickType.QUICK_CRAFT,
                    player
            );
        }
        player.inventoryMenu.clicked(
                AbstractContainerMenu.SLOT_CLICKED_OUTSIDE,
                AbstractContainerMenu.getQuickcraftMask(2, type),
                ClickType.QUICK_CRAFT,
                player
        );
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

    private static final class CapturingSynchronizer implements ContainerSynchronizer {
        private NonNullList<ItemStack> initialItems = NonNullList.create();
        private int changedSlot = -1;
        private ItemStack changedStack = ItemStack.EMPTY;

        @Override
        public void sendInitialData(
                AbstractContainerMenu menu,
                NonNullList<ItemStack> items,
                ItemStack carried,
                int[] data
        ) {
            this.initialItems = NonNullList.withSize(items.size(), ItemStack.EMPTY);
            for (int slot = 0; slot < items.size(); slot++) {
                this.initialItems.set(slot, items.get(slot).copy());
            }
        }

        @Override
        public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack) {
            this.changedSlot = slot;
            this.changedStack = stack.copy();
        }

        @Override
        public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) {
        }

        @Override
        public void sendDataChange(AbstractContainerMenu menu, int id, int value) {
        }

        private void clearSlotChange() {
            this.changedSlot = -1;
            this.changedStack = ItemStack.EMPTY;
        }
    }
}
