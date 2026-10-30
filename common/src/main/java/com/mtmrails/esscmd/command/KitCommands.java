package com.mtmrails.esscmd.command;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mtmrails.esscmd.compat.ItemData;
import com.mtmrails.esscmd.compat.ItemLookup;
import com.mtmrails.esscmd.compat.Texts;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.Kit;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.platform.Platform;
import com.mtmrails.esscmd.util.DateDiff;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** {@code /kit [kit] [player]}, {@code /createkit}, {@code /delkit}, {@code /showkit}, {@code /kitreset}. */
public final class KitCommands {
    private static final SuggestionProvider<CommandSourceStack> KITS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(EssData.get(ctx.getSource().getServer()).kits().keySet().stream()
                    .filter(name -> canUse(ctx.getSource(), name, EssData.get(ctx.getSource().getServer()).kits().get(name))), builder);

    private KitCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kit")
                .requires(PermissionNodes.KIT.require())
                .executes(ctx -> list(ctx.getSource()))
                .then(Commands.argument("kit", StringArgumentType.word())
                        .suggests(KITS)
                        .executes(ctx -> claim(ctx.getSource(), StringArgumentType.getString(ctx, "kit")))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(PermissionNodes.KIT_OTHERS.require())
                                .executes(ctx -> give(ctx.getSource(), StringArgumentType.getString(ctx, "kit"),
                                        EntityArgument.getPlayer(ctx, "player"))))));
        LiteralCommandNode<CommandSourceStack> create = dispatcher.register(Commands.literal("createkit")
                .requires(PermissionNodes.CREATEKIT.require())
                .then(Commands.argument("kit", StringArgumentType.word())
                        .then(Commands.argument("delay", StringArgumentType.word())
                                .executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "kit"),
                                        StringArgumentType.getString(ctx, "delay"), 0))
                                .then(Commands.argument("opLevel", IntegerArgumentType.integer(0, 4))
                                        .executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "kit"),
                                                StringArgumentType.getString(ctx, "delay"),
                                                IntegerArgumentType.getInteger(ctx, "opLevel")))))));
        for (String alias : List.of("ck", "kc")) {
            dispatcher.register(Commands.literal(alias).requires(create.getRequirement()).redirect(create));
        }
        dispatcher.register(Commands.literal("delkit")
                .requires(PermissionNodes.DELKIT.require())
                .then(Commands.argument("kit", StringArgumentType.word())
                        .suggests(KITS)
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "kit").toLowerCase(Locale.ROOT);
                            return EssData.get(ctx.getSource().getServer()).removeKit(name)
                                    ? CommandUtil.ok(ctx.getSource(), "kit.deleted", name)
                                    : CommandUtil.fail(ctx.getSource(), "kit.not-found", name);
                        })));
        LiteralCommandNode<CommandSourceStack> show = dispatcher.register(Commands.literal("showkit")
                .requires(PermissionNodes.SHOWKIT.require())
                .then(Commands.argument("kit", StringArgumentType.word())
                        .suggests(KITS)
                        .executes(ctx -> show(ctx.getSource(), StringArgumentType.getString(ctx, "kit")))));
        for (String alias : List.of("kitshow", "preview", "kitpreview")) {
            dispatcher.register(Commands.literal(alias).requires(show.getRequirement()).redirect(show));
        }
        dispatcher.register(Commands.literal("kitreset")
                .requires(PermissionNodes.KITRESET.require())
                .then(Commands.argument("kit", StringArgumentType.word())
                        .suggests(KITS)
                        .executes(ctx -> reset(ctx.getSource(), StringArgumentType.getString(ctx, "kit"),
                                ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> reset(ctx.getSource(), StringArgumentType.getString(ctx, "kit"),
                                        EntityArgument.getPlayer(ctx, "player"))))));
    }

    /** The kit's OP level, or {@code cmdessentials.kits.<name>} from a permission provider. */
    private static boolean canUse(CommandSourceStack source, String name, Kit kit) {
        return Platform.get().hasPermission(source, "cmdessentials.kits." + name, kit.opLevel());
    }

    /** kits.skipUsedOneTimeKits: hide one-time kits the player has already claimed. */
    private static boolean usedOnce(CommandSourceStack source, String name, Kit kit) {
        return ConfigManager.get().kits.skipUsedOneTimeKits && kit.delaySeconds() < 0
                && source.getEntity() instanceof ServerPlayer player
                && EssData.get(source.getServer()).kitUsed(player.getUUID(), name) > 0;
    }

    private static Kit find(CommandSourceStack source, String name) {
        Kit kit = EssData.get(source.getServer()).kits().get(name);
        if (kit == null) {
            CommandUtil.fail(source, "kit.not-found", name);
        }
        return kit;
    }

    private static int list(CommandSourceStack source) {
        var kits = EssData.get(source.getServer()).kits();
        List<String> visible = kits.keySet().stream()
                .filter(name -> canUse(source, name, kits.get(name)) && !usedOnce(source, name, kits.get(name))).toList();
        return visible.isEmpty() ? CommandUtil.fail(source, "kit.none")
                : CommandUtil.ok(source, "kit.list", String.join(", ", visible));
    }

    private static int claim(CommandSourceStack source, String rawName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String name = rawName.toLowerCase(Locale.ROOT);
        Kit kit = find(source, name);
        if (kit == null) {
            return 0;
        }
        if (!canUse(source, name, kit)) {
            return CommandUtil.fail(source, "kit.no-access", name);
        }
        EssData data = EssData.get(source.getServer());
        long last = data.kitUsed(player.getUUID(), name);
        long now = System.currentTimeMillis();
        if (last > 0 && !PermissionNodes.KITRESET.check(source)) {
            if (kit.delaySeconds() < 0) {
                return CommandUtil.fail(source, "kit.once", name);
            }
            long readyAt = last + kit.delaySeconds() * 1000;
            if (now < readyAt) {
                return CommandUtil.fail(source, "kit.cooldown", name, DateDiff.format(Duration.ofMillis(readyAt - now + 999)));
            }
        }
        if (!ConfigManager.get().kits.dropItemsIfFull && freeSlots(player) < kit.items().size()) {
            return CommandUtil.fail(source, "kit.no-room", name);
        }
        if (giveItems(source.getServer(), kit, player) == 0) {
            return CommandUtil.fail(source, "kit.broken", name);
        }
        data.setKitUsed(player.getUUID(), name, now);
        return CommandUtil.ok(source, "kit.received", name);
    }

    /** /kit <kit> <player>: an admin action, so no access check or cooldown. */
    private static int give(CommandSourceStack source, String rawName, ServerPlayer target) {
        String name = rawName.toLowerCase(Locale.ROOT);
        Kit kit = find(source, name);
        if (kit == null) {
            return 0;
        }
        if (giveItems(source.getServer(), kit, target) == 0) {
            return CommandUtil.fail(source, "kit.broken", name);
        }
        CommandUtil.tell(target, "kit.received", name);
        return CommandUtil.ok(source, "kit.given", name, target.getGameProfile().getName());
    }

    private static int freeSlots(ServerPlayer player) {
        int free = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                free++;
            }
        }
        return free;
    }

    /** Fills the inventory and drops what doesn't fit, so nothing in the kit is lost. */
    private static int giveItems(MinecraftServer server, Kit kit, ServerPlayer player) {
        int given = 0;
        for (CompoundTag tag : kit.items()) {
            Optional<ItemStack> stack = ItemData.load(server, tag);
            if (stack.isPresent()) {
                ItemStack item = stack.get();
                player.getInventory().add(item);
                if (!item.isEmpty()) {
                    player.drop(item, false);
                }
                given++;
            }
        }
        player.containerMenu.broadcastChanges();
        return given;
    }

    /** Seconds, -1 for a single claim, or a duration such as {@code 1d2h}. Null if invalid. */
    private static Long parseDelay(String text) {
        if (text.matches("-?\\d{1,12}")) {
            return Long.parseLong(text);
        }
        return DateDiff.parse(text).map(Duration::getSeconds).orElse(null);
    }

    private static int create(CommandSourceStack source, String rawName, String rawDelay, int opLevel) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String name = CommandUtil.validName(source, rawName);
        if (name == null) {
            return 0;
        }
        Long delay = parseDelay(rawDelay);
        if (delay == null) {
            return CommandUtil.fail(source, "kit.invalid-delay", rawDelay);
        }
        Inventory inventory = player.getInventory();
        List<CompoundTag> items = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                items.add(ItemData.save(source.getServer(), stack));
            }
        }
        if (items.isEmpty()) {
            return CommandUtil.fail(source, "kit.empty");
        }
        EssData.get(source.getServer()).setKit(name, new Kit(delay, opLevel, List.copyOf(items)));
        return CommandUtil.ok(source, "kit.created", name, items.size());
    }

    private static int show(CommandSourceStack source, String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        Kit kit = find(source, name);
        if (kit == null) {
            return 0;
        }
        if (!canUse(source, name, kit)) {
            return CommandUtil.fail(source, "kit.no-access", name);
        }
        MutableComponent contents = Texts.empty();
        boolean first = true;
        for (CompoundTag tag : kit.items()) {
            Optional<ItemStack> stack = ItemData.load(source.getServer(), tag);
            if (stack.isPresent()) {
                contents.append(Texts.literal(first ? "" : ", "));
                contents.append(Texts.literal(ItemLookup.id(stack.get().getItem()).toString()).withStyle(ChatFormatting.GOLD));
                contents.append(Texts.literal(" x" + stack.get().getCount()));
                first = false;
            }
        }
        return CommandUtil.ok(source, "kit.contents", name, contents);
    }

    private static int reset(CommandSourceStack source, String rawName, ServerPlayer target) {
        String name = rawName.toLowerCase(Locale.ROOT);
        if (find(source, name) == null) {
            return 0;
        }
        EssData.get(source.getServer()).resetKit(target.getUUID(), name);
        return CommandUtil.ok(source, "kit.reset", name, target.getGameProfile().getName());
    }
}
