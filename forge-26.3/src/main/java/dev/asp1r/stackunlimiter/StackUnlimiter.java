package dev.asp1r.stackunlimiter;

import com.mojang.logging.LogUtils;
import dev.asp1r.stackunlimiter.gametest.StackCountGameTests;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.gametest.ForgeGameTestHooks;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;

@Mod(StackUnlimiter.MOD_ID)
public final class StackUnlimiter {
    public static final String MOD_ID = "stack_unlimiter";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StackUnlimiter(FMLJavaModLoadingContext context) {
        var gameTests = ForgeGameTestHooks.gatherTests(StackCountGameTests.class, null);
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            if (event.getRegistryKey() == Registries.TEST_FUNCTION) {
                gameTests.forEach((id, reference) -> event.register(
                        Registries.TEST_FUNCTION,
                        id,
                        reference::consumer
                ));
            }
        });
        LOGGER.info("Stack Unlimiter is active; exact stack counts use signed 64-bit storage");
    }
}
