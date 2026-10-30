package com.mtmrails.esscmd.command;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.StoredLocation;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.platform.Platform;
import com.mtmrails.esscmd.teleport.TeleportService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /sethome}, {@code /home}, {@code /delhome}, {@code /renamehome}. No bed needed, works
 * across dimensions. {@code player:name} manages another player's homes (needs home.others).
 */
public final class HomeCommands {
    private static final String DEFAULT_NAME = "home";

    private static final SuggestionProvider<CommandSourceStack> OWN_HOMES = (ctx, builder) -> {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return builder.buildFuture();
        }
        return SharedSuggestionProvider.suggest(EssData.get(ctx.getSource().getServer()).homes(player.getUUID()).keySet(), builder);
    };

    /** Player -> home name awaiting a second /sethome (homes.confirmOverwrite). */
    private static final Map<UUID, String> PENDING_OVERWRITE = new HashMap<>();

    private HomeCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sethome")
                .requires(PermissionNodes.SETHOME.require())
                .executes(ctx -> setHome(ctx.getSource(), DEFAULT_NAME))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> setHome(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        dispatcher.register(Commands.literal("home")
                .requires(PermissionNodes.HOME.require())
                .executes(ctx -> home(ctx.getSource(), null))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests(OWN_HOMES)
                        .executes(ctx -> home(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        dispatcher.register(Commands.literal("delhome")
                .requires(PermissionNodes.DELHOME.require())
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests(OWN_HOMES)
                        .executes(ctx -> delHome(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        dispatcher.register(Commands.literal("renamehome")
                .requires(PermissionNodes.RENAMEHOME.require())
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(OWN_HOMES)
                        .then(Commands.argument("newName", StringArgumentType.string())
                                .executes(ctx -> renameHome(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"),
                                        StringArgumentType.getString(ctx, "newName"))))));
    }

    /** Resolves {@code [player:]name}, checking home.others for another player's homes. */
    private static CommandUtil.OwnedName resolve(CommandSourceStack source, ServerPlayer self, String input) {
        CommandUtil.OwnedName owned = CommandUtil.ownedName(source, self, input);
        if (owned != null && owned.other() && !PermissionNodes.HOME_OTHERS.check(source)) {
            CommandUtil.fail(source, "error.no-permission-others");
            return null;
        }
        return owned;
    }

    private static int setHome(CommandSourceStack source, String input) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CommandUtil.OwnedName owned = resolve(source, player, input);
        if (owned == null) {
            return 0;
        }
        String name = CommandUtil.validName(source, owned.name().isEmpty() ? DEFAULT_NAME : owned.name());
        if (name == null) {
            return 0;
        }
        EssData data = EssData.get(source.getServer());
        Map<String, StoredLocation> homes = data.homes(owned.owner());
        if (!owned.other() && !homes.containsKey(name)) {
            int limit = Platform.get().getInt(player, PermissionNodes.HOME_LIMIT);
            if (limit >= 0 && homes.size() >= limit) {
                return CommandUtil.fail(source, "home.limit", limit);
            }
        }
        if (ConfigManager.get().homes.confirmOverwrite && homes.containsKey(name)
                && !name.equals(PENDING_OVERWRITE.remove(player.getUUID()))) {
            PENDING_OVERWRITE.put(player.getUUID(), name);
            return CommandUtil.ok(source, "home.confirm-overwrite", name);
        }
        data.setHome(owned.owner(), name, StoredLocation.of(player));
        return owned.other()
                ? CommandUtil.ok(source, "home.set-other", name, owned.ownerName())
                : CommandUtil.ok(source, "home.set", name);
    }

    private static int home(CommandSourceStack source, String input) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CommandUtil.OwnedName owned = resolve(source, player, input == null ? "" : input);
        if (owned == null) {
            return 0;
        }
        Map<String, StoredLocation> homes = EssData.get(source.getServer()).homes(owned.owner());
        String name = owned.name();
        if (name.isEmpty()) {
            if (homes.isEmpty()) {
                if (owned.other()) {
                    return CommandUtil.fail(source, "home.none-other", owned.ownerName());
                }
                if (ConfigManager.get().homes.spawnIfNoHome) {
                    return TeleportService.teleport(player, () -> SpawnCommands.spawn(player.server)) ? 1 : 0;
                }
                return CommandUtil.fail(source, "home.none");
            }
            if (homes.size() > 1 || owned.other()) {
                return CommandUtil.ok(source, "home.list", String.join(", ", homes.keySet()));
            }
            name = homes.keySet().iterator().next();
        }
        StoredLocation home = homes.get(name.toLowerCase(Locale.ROOT));
        if (home == null) {
            return CommandUtil.fail(source, "home.not-found", name);
        }
        return TeleportService.teleport(player, () -> home) ? 1 : 0;
    }

    private static int delHome(CommandSourceStack source, String input) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CommandUtil.OwnedName owned = resolve(source, player, input);
        if (owned == null) {
            return 0;
        }
        String name = owned.name().toLowerCase(Locale.ROOT);
        if (!EssData.get(source.getServer()).removeHome(owned.owner(), name)) {
            return CommandUtil.fail(source, "home.not-found", owned.name());
        }
        return CommandUtil.ok(source, "home.deleted", name);
    }

    private static int renameHome(CommandSourceStack source, String input, String newInput) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CommandUtil.OwnedName owned = resolve(source, player, input);
        if (owned == null) {
            return 0;
        }
        String newName = CommandUtil.validName(source, newInput);
        if (newName == null) {
            return 0;
        }
        String name = owned.name().toLowerCase(Locale.ROOT);
        EssData data = EssData.get(source.getServer());
        Map<String, StoredLocation> homes = data.homes(owned.owner());
        StoredLocation home = homes.get(name);
        if (home == null) {
            return CommandUtil.fail(source, "home.not-found", owned.name());
        }
        if (homes.containsKey(newName)) {
            return CommandUtil.fail(source, "home.exists", newName);
        }
        data.removeHome(owned.owner(), name);
        data.setHome(owned.owner(), newName, home);
        return CommandUtil.ok(source, "home.renamed", name, newName);
    }
}
