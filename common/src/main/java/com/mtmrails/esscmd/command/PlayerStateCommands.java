package com.mtmrails.esscmd.command;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mtmrails.esscmd.compat.Effects;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.permission.PermissionNode;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.player.AbilityService;
import com.mtmrails.esscmd.player.VanishService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /fly}, {@code /god} (persistent toggles), {@code /heal}, {@code /feed}, {@code /rest},
 * {@code /ext} (one-off actions) and {@code /suicide}. All but /suicide take {@code [player]}.
 */
public final class PlayerStateCommands {
    private static final int MAX_FOOD = 20;
    private static final float FULL_SATURATION = 20f;
    private static final float FEED_SATURATION = 10f;

    /** Player -> server tick when /heal is ready again. */
    private static final Map<UUID, Integer> HEAL_READY = new HashMap<>();

    private PlayerStateCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(toggle("fly", PermissionNodes.FLY, PermissionNodes.FLY_OTHERS,
                (source, player) -> EssData.get(source.getServer()).fly(player.getUUID()),
                (player, on) -> EssData.get(player.server).setFly(player.getUUID(), on)));
        dispatcher.register(toggle("god", PermissionNodes.GOD, PermissionNodes.GOD_OTHERS,
                (source, player) -> EssData.get(source.getServer()).god(player.getUUID()),
                (player, on) -> EssData.get(player.server).setGod(player.getUUID(), on)));
        dispatcher.register(action("heal", PermissionNodes.HEAL, PermissionNodes.HEAL_OTHERS, player -> {
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(MAX_FOOD);
            player.getFoodData().setSaturation(FULL_SATURATION);
            player.clearFire();
            if (ConfigManager.get().heal.removeEffects) {
                Effects.removeHarmful(player);
            }
        }));
        dispatcher.register(action("feed", PermissionNodes.FEED, PermissionNodes.FEED_OTHERS, player -> {
            player.getFoodData().setFoodLevel(MAX_FOOD);
            player.getFoodData().setSaturation(FEED_SATURATION);
        }));
        dispatcher.register(action("rest", PermissionNodes.REST, PermissionNodes.REST_OTHERS,
                player -> player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST))));
        dispatcher.register(action("ext", PermissionNodes.EXT, PermissionNodes.EXT_OTHERS, ServerPlayer::clearFire));
        dispatcher.register(toggle("vanish", PermissionNodes.VANISH, PermissionNodes.VANISH_OTHERS,
                (source, player) -> VanishService.isVanished(player), VanishService::set));
        dispatcher.register(Commands.literal("suicide")
                .requires(PermissionNodes.SUICIDE.require())
                .executes(ctx -> {
                    ctx.getSource().getPlayerOrException().kill();
                    return 1;
                }));
    }

    private static String name(ServerPlayer player) {
        return player.getGameProfile().getName();
    }

    /** Tells the source; tells the target too when it's someone else. Message key: {@code <command>.done}. */
    private static void report(CommandSourceStack source, ServerPlayer target, String key) {
        CommandUtil.ok(source, key, name(target));
        if (source.getEntity() != target) {
            CommandUtil.tell(target, key, name(target));
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> action(String command, PermissionNode node,
                                                                     PermissionNode others, Consumer<ServerPlayer> effect) {
        return Commands.literal(command)
                .requires(node.require())
                .executes(ctx -> act(ctx.getSource(), ctx.getSource().getPlayerOrException(), command, effect))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(others.require())
                        .executes(ctx -> act(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), command, effect)));
    }

    private static int act(CommandSourceStack source, ServerPlayer target, String command, Consumer<ServerPlayer> effect) {
        if (command.equals("heal") && source.getEntity() == target && !PermissionNodes.HEAL_COOLDOWN_BYPASS.check(source)) {
            int now = source.getServer().getTickCount();
            Integer readyAt = HEAL_READY.get(target.getUUID());
            if (readyAt != null && now < readyAt) {
                return CommandUtil.fail(source, "heal.cooldown", (readyAt - now + 19) / 20);
            }
            HEAL_READY.put(target.getUUID(), now + ConfigManager.get().heal.cooldownSeconds * 20);
        }
        effect.accept(target);
        report(source, target, command + ".done");
        return 1;
    }

    /** Reads the current state of a toggle for a player. */
    private interface ToggleState {
        boolean get(CommandSourceStack source, ServerPlayer player);
    }

    /** {@code /<command> [player] [on|off]}; without on/off it flips. Keys: {@code <command>.on/off}. */
    private static LiteralArgumentBuilder<CommandSourceStack> toggle(String command, PermissionNode node, PermissionNode others,
                                                                     ToggleState state, BiConsumer<ServerPlayer, Boolean> store) {
        return Commands.literal(command)
                .requires(node.require())
                .executes(ctx -> set(ctx.getSource(), ctx.getSource().getPlayerOrException(), null, command, state, store))
                .then(Commands.literal("on").executes(ctx ->
                        set(ctx.getSource(), ctx.getSource().getPlayerOrException(), true, command, state, store)))
                .then(Commands.literal("off").executes(ctx ->
                        set(ctx.getSource(), ctx.getSource().getPlayerOrException(), false, command, state, store)))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(others.require())
                        .executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), null, command, state, store))
                        .then(Commands.literal("on").executes(ctx ->
                                set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), true, command, state, store)))
                        .then(Commands.literal("off").executes(ctx ->
                                set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), false, command, state, store))));
    }

    private static int set(CommandSourceStack source, ServerPlayer target, @Nullable Boolean wanted, String command,
                           ToggleState state, BiConsumer<ServerPlayer, Boolean> store) throws CommandSyntaxException {
        boolean on = wanted != null ? wanted : !state.get(source, target);
        store.accept(target, on);
        AbilityService.refresh(target);
        report(source, target, command + (on ? ".on" : ".off"));
        return 1;
    }
}
