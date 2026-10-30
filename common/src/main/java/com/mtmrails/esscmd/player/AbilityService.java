package com.mtmrails.esscmd.player;

import com.mtmrails.esscmd.data.EssData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;

/**
 * Keeps {@code /fly} and {@code /god} in effect. Vanilla resets abilities on respawn and on
 * every gamemode change, so instead of hooking each of those we re-check once a second.
 */
public final class AbilityService {
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int MAX_FOOD = 20;

    private AbilityService() {}

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        EssData data = EssData.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean fly = data.fly(player.getUUID());
            boolean god = data.god(player.getUUID());
            if (fly || god) {
                apply(player, fly, god);
            }
        }
    }

    /** Applies the stored flags now, e.g. right after /fly or /god changed them. */
    public static void refresh(ServerPlayer player) {
        EssData data = EssData.get(player.server);
        Abilities abilities = player.getAbilities();
        // Start from what the gamemode grants, so turning a flag off doesn't take away
        // creative flight or invulnerability.
        player.gameMode.getGameModeForPlayer().updatePlayerAbilities(abilities);
        apply(player, data.fly(player.getUUID()), data.god(player.getUUID()));
        if (!abilities.mayfly && abilities.flying) {
            abilities.flying = false;
        }
        player.onUpdateAbilities();
    }

    private static void apply(ServerPlayer player, boolean fly, boolean god) {
        Abilities abilities = player.getAbilities();
        boolean changed = false;
        if (fly && !abilities.mayfly) {
            abilities.mayfly = true;
            changed = true;
        }
        if (god && !abilities.invulnerable) {
            abilities.invulnerable = true;
            changed = true;
        }
        if (changed) {
            player.onUpdateAbilities();
        }
        if (god && player.getFoodData().getFoodLevel() < MAX_FOOD) {
            player.getFoodData().setFoodLevel(MAX_FOOD);
        }
    }
}
