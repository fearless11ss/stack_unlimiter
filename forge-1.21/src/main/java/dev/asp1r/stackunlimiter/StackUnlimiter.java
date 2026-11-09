package dev.asp1r.stackunlimiter;

import com.mojang.logging.LogUtils;
import dev.asp1r.stackunlimiter.gametest.StackCountGameTests;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.ForgeGameTestHooks;
import org.slf4j.Logger;

@Mod(StackUnlimiter.MOD_ID)
public final class StackUnlimiter {
    public static final String MOD_ID = "stack_unlimiter";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("deprecation")
    public StackUnlimiter() {
        if (ForgeGameTestHooks.isGametestEnabled()) {
            GameTestRegistry.register(StackCountGameTests.class);
            LOGGER.info("Registered Stack Unlimiter game tests");
        }
        LOGGER.info("Stack Unlimiter is active; exact stack counts use signed 64-bit storage");
    }
}
