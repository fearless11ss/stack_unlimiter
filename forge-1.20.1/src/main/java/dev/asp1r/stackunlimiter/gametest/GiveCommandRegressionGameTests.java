package dev.asp1r.stackunlimiter.gametest;

import com.mojang.authlib.GameProfile;
import dev.asp1r.stackunlimiter.StackUnlimiter;
import dev.asp1r.stackunlimiter.api.StackCounts;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(StackUnlimiter.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GiveCommandRegressionGameTests {
    private GiveCommandRegressionGameTests() {
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void giveCommandPreservesNonStackableItems(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper);

        performCommand(player, "give @s minecraft:diamond_pickaxe");
        performCommand(player, "give @s minecraft:shulker_box");

        assertEquals(1, countInventoryItem(player, Items.DIAMOND_PICKAXE),
                "server pickaxe count after /give");
        assertEquals(1, countInventoryItem(player, Items.SHULKER_BOX),
                "server shulker-box count after /give");
        assertEquals(0, StackCounts.get(ItemStack.EMPTY),
                "shared empty-stack count after /give");

        ClientboundContainerSetContentPacket decoded = roundTripMenu(player);
        long clientPickaxes = 0;
        long clientShulkerBoxes = 0;
        for (ItemStack stack : decoded.getItems()) {
            if (stack.is(Items.DIAMOND_PICKAXE)) {
                clientPickaxes += StackCounts.get(stack);
            } else if (stack.is(Items.SHULKER_BOX)) {
                clientShulkerBoxes += StackCounts.get(stack);
            }
        }
        assertEquals(1, clientPickaxes, "client pickaxe count after synchronization");
        assertEquals(1, clientShulkerBoxes, "client shulker-box count after synchronization");
        helper.succeed();
    }

    private static ServerPlayer createPlayer(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "stack-unlimiter-give-regression")
        );
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        player.connection = new ServerGamePacketListenerImpl(
                helper.getLevel().getServer(),
                connection,
                player
        );
        connection.setListener(player.connection);
        player.getInventory().clearContent();
        return player;
    }

    private static void performCommand(ServerPlayer player, String command) {
        try {
            player.getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack().withPermission(4),
                    command
            );
        } catch (NullPointerException exception) {
            if (exception.getMessage() == null
                    || (!exception.getMessage().contains("channel")
                    && !exception.getMessage().contains("connection"))) {
                throw exception;
            }
        }
    }

    private static ClientboundContainerSetContentPacket roundTripMenu(ServerPlayer player) {
        ClientboundContainerSetContentPacket outgoing = new ClientboundContainerSetContentPacket(
                player.inventoryMenu.containerId,
                player.inventoryMenu.getStateId(),
                player.inventoryMenu.getItems(),
                player.inventoryMenu.getCarried()
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            outgoing.write(buffer);
            return new ClientboundContainerSetContentPacket(buffer);
        } finally {
            buffer.release();
        }
    }

    private static long countInventoryItem(ServerPlayer player, Item item) {
        long total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += StackCounts.get(stack);
            }
        }
        return total;
    }

    private static void assertEquals(long expected, long actual, String subject) {
        if (expected != actual) {
            throw new GameTestAssertException(
                    subject + ": expected " + expected + ", got " + actual
            );
        }
    }
}
