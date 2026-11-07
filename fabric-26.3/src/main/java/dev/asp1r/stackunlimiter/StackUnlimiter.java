package dev.asp1r.stackunlimiter;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;

public final class StackUnlimiter implements ModInitializer {
    public static final String MOD_ID = "stack_unlimiter";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOGGER.info("Stack Unlimiter is active; exact stack counts use signed 64-bit storage");
    }
}
