package dev.asp1r.stackunlimiter;

import com.mojang.logging.LogUtils;
import dev.asp1r.stackunlimiter.gametest.StackCountGameTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.slf4j.Logger;

@Mod(StackUnlimiter.MOD_ID)
public final class StackUnlimiter {
    public static final String MOD_ID = "stack_unlimiter";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StackUnlimiter(IEventBus modBus) {
        modBus.addListener(StackUnlimiter::registerGameTests);
        LOGGER.info("Stack Unlimiter is active; exact stack counts use signed 64-bit storage");
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        event.register(StackCountGameTests.class);
        LOGGER.info("Registered Stack Unlimiter game tests");
    }
}
