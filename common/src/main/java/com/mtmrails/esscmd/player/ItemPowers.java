package com.mtmrails.esscmd.player;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mtmrails.esscmd.compat.ItemLookup;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.PlayerRecord;
import com.mtmrails.esscmd.message.Messages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** /powertool commands on right-click, and /unlimited refilling held stacks once a second. */
public final class ItemPowers {
    private static final int REFILL_INTERVAL_TICKS = 20;

    private ItemPowers() {}

    /** True if {@code stack} is a powertool and its command ran. */
    public static boolean runPowerTool(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        PlayerRecord record = EssData.get(player.server).player(player.getUUID()).orElse(null);
        if (record == null || record.powerToolsOff()) {
            return false;
        }
        String command = record.powerTools().get(ItemLookup.id(stack.getItem()).toString());
        if (command == null) {
            return false;
        }
        try {
            player.server.getCommands().getDispatcher().execute(command, player.createCommandSourceStack());
        } catch (CommandSyntaxException e) {
            Players.send(player, Messages.error(player, "powertool.failed", e.getMessage()));
        }
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % REFILL_INTERVAL_TICKS != 0) {
            return;
        }
        EssData data = EssData.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            data.player(player.getUUID()).filter(record -> !record.unlimited().isEmpty()).ifPresent(record -> {
                refill(player.getMainHandItem(), record);
                refill(player.getOffhandItem(), record);
            });
        }
    }

    private static void refill(ItemStack stack, PlayerRecord record) {
        if (!stack.isEmpty() && record.unlimited().contains(ItemLookup.id(stack.getItem()).toString())) {
            stack.setCount(stack.getMaxStackSize());
        }
    }
}
