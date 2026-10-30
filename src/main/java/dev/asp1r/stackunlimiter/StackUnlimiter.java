package dev.asp1r.stackunlimiter;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(StackUnlimiter.MOD_ID)
public final class StackUnlimiter {
    public static final String MOD_ID = "stack_unlimiter";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StackUnlimiter() {
        LOGGER.info("Stack Unlimiter is active; exact stack counts use signed 64-bit storage");
    }
}
