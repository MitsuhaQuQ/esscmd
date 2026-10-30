package com.mtmrails.esscmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mtmrails.esscmd.command.EssentialsCommand;
import com.mtmrails.esscmd.platform.Platform;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loader-independent entry point. Loader modules forward their events here. */
public final class CmdEssentials {
    public static final String MOD_ID = "cmdessentials";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private CmdEssentials() {}

    public static void init() {
        LOGGER.info("cmdEssentials {} loading on {}", Platform.get().modVersion(), Platform.get().loaderName());
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        EssentialsCommand.register(dispatcher);
    }
}
