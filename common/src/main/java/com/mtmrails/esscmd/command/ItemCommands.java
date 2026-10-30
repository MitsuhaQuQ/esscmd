package com.mtmrails.esscmd.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mtmrails.esscmd.compat.Dimensions;
import com.mtmrails.esscmd.compat.ItemData;
import com.mtmrails.esscmd.compat.ItemLookup;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.compat.Signs;
import com.mtmrails.esscmd.compat.Texts;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.PlayerRecord;
import com.mtmrails.esscmd.permission.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * {@code /item}, {@code /itemdb}, {@code /itemname}, {@code /itemlore}, {@code /exp},
 * {@code /skull}, {@code /condense}, {@code /editsign}, {@code /unlimited},
 * {@code /powertool}, {@code /powertoollist}, {@code /powertooltoggle}. Items are standard ids.
 */
public final class ItemCommands {
    private static final int LOOK_RANGE = 6;
    private static final int CONDENSE_RATIO = 9;

    private static final SuggestionProvider<CommandSourceStack> ITEMS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(ItemLookup.ids(), builder);

    private ItemCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> item = dispatcher.register(Commands.literal("item")
                .requires(PermissionNodes.ITEM.require())
                .then(Commands.argument("item", ResourceLocationArgument.id()).suggests(ITEMS)
                        .executes(ctx -> give(ctx.getSource(), ResourceLocationArgument.getId(ctx, "item"), 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 6400))
                                .executes(ctx -> give(ctx.getSource(), ResourceLocationArgument.getId(ctx, "item"),
                                        IntegerArgumentType.getInteger(ctx, "amount"))))));
        dispatcher.register(Commands.literal("i").requires(item.getRequirement()).redirect(item));
        dispatcher.register(Commands.literal("itemdb")
                .requires(PermissionNodes.ITEMDB.require())
                .executes(ctx -> itemInfo(ctx.getSource(), ctx.getSource().getPlayerOrException().getMainHandItem().getItem()))
                .then(Commands.argument("item", ResourceLocationArgument.id()).suggests(ITEMS)
                        .executes(ctx -> {
                            ResourceLocation id = ResourceLocationArgument.getId(ctx, "item");
                            Optional<Item> found = ItemLookup.find(id);
                            return found.isPresent() ? itemInfo(ctx.getSource(), found.get())
                                    : CommandUtil.fail(ctx.getSource(), "item.unknown", id);
                        })));
        dispatcher.register(Commands.literal("itemname")
                .requires(PermissionNodes.ITEMNAME.require())
                .executes(ctx -> rename(ctx.getSource(), null))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> rename(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        dispatcher.register(Commands.literal("itemlore")
                .requires(PermissionNodes.ITEMLORE.require())
                .then(Commands.literal("add").then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> loreAdd(ctx.getSource(), StringArgumentType.getString(ctx, "text")))))
                .then(Commands.literal("set").then(Commands.argument("line", IntegerArgumentType.integer(1))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> loreSet(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "line"),
                                        StringArgumentType.getString(ctx, "text"))))))
                .then(Commands.literal("clear").executes(ctx -> loreClear(ctx.getSource()))));
        dispatcher.register(Commands.literal("exp")
                .requires(PermissionNodes.EXP.require())
                .executes(ctx -> expShow(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("show").executes(ctx -> expShow(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(PermissionNodes.EXP_OTHERS.require())
                                .executes(ctx -> expShow(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player())
                        .requires(PermissionNodes.EXP_OTHERS.require())
                        .executes(ctx -> expSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), 0, false))))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                        .requires(PermissionNodes.EXP_OTHERS.require())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> expSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                        IntegerArgumentType.getInteger(ctx, "amount"), false)))))
                .then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player())
                        .requires(PermissionNodes.EXP_OTHERS.require())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> expSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                        IntegerArgumentType.getInteger(ctx, "amount"), true))))));
        dispatcher.register(Commands.literal("skull")
                .requires(PermissionNodes.SKULL.require())
                .executes(ctx -> skull(ctx.getSource(), ctx.getSource().getPlayerOrException().getGameProfile().getName(), null))
                .then(Commands.argument("owner", StringArgumentType.word())
                        .executes(ctx -> skull(ctx.getSource(), StringArgumentType.getString(ctx, "owner"), null))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> skull(ctx.getSource(), StringArgumentType.getString(ctx, "owner"),
                                        EntityArgument.getPlayer(ctx, "player"))))));
        dispatcher.register(Commands.literal("condense")
                .requires(PermissionNodes.CONDENSE.require())
                .executes(ctx -> condense(ctx.getSource())));
        LiteralCommandNode<CommandSourceStack> sign = dispatcher.register(Commands.literal("editsign")
                .requires(PermissionNodes.EDITSIGN.require())
                .then(Commands.literal("set").then(Commands.argument("line", IntegerArgumentType.integer(1, 4))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> editSign(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "line"),
                                        StringArgumentType.getString(ctx, "text"))))))
                .then(Commands.literal("clear").then(Commands.argument("line", IntegerArgumentType.integer(1, 4))
                        .executes(ctx -> editSign(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "line"), "")))));
        dispatcher.register(Commands.literal("sign").requires(sign.getRequirement()).redirect(sign));
        dispatcher.register(Commands.literal("unlimited")
                .requires(PermissionNodes.UNLIMITED.require())
                .then(Commands.literal("list").executes(ctx -> unlimitedList(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("clear").executes(ctx -> {
                    EssData.get(ctx.getSource().getServer()).clearUnlimited(ctx.getSource().getPlayerOrException().getUUID());
                    return CommandUtil.ok(ctx.getSource(), "unlimited.cleared");
                }))
                .then(Commands.argument("item", ResourceLocationArgument.id()).suggests(ITEMS)
                        .executes(ctx -> unlimited(ctx.getSource(), ResourceLocationArgument.getId(ctx, "item"),
                                ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(PermissionNodes.UNLIMITED_OTHERS.require())
                                .executes(ctx -> unlimited(ctx.getSource(), ResourceLocationArgument.getId(ctx, "item"),
                                        EntityArgument.getPlayer(ctx, "player"))))));
        LiteralCommandNode<CommandSourceStack> tool = dispatcher.register(Commands.literal("powertool")
                .requires(PermissionNodes.POWERTOOL.require())
                .executes(ctx -> powerTool(ctx.getSource(), null))
                .then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(ctx -> powerTool(ctx.getSource(), StringArgumentType.getString(ctx, "command")))));
        dispatcher.register(Commands.literal("pt").requires(tool.getRequirement()).redirect(tool));
        dispatcher.register(Commands.literal("powertoollist")
                .requires(PermissionNodes.POWERTOOL.require())
                .executes(ctx -> powerToolList(ctx.getSource())));
        dispatcher.register(Commands.literal("powertooltoggle")
                .requires(PermissionNodes.POWERTOOL.require())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    EssData data = EssData.get(player.server);
                    boolean off = !data.player(player.getUUID()).map(PlayerRecord::powerToolsOff).orElse(false);
                    data.setPowerToolsOff(player.getUUID(), off);
                    return CommandUtil.ok(ctx.getSource(), off ? "powertool.toggle-off" : "powertool.toggle-on");
                }));
    }

    private static ItemStack held(CommandSourceStack source) throws CommandSyntaxException {
        ItemStack stack = source.getPlayerOrException().getMainHandItem();
        if (stack.isEmpty()) {
            CommandUtil.fail(source, "item.empty-hand");
            return null;
        }
        return stack;
    }

    private static void addOrDrop(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    private static int give(CommandSourceStack source, ResourceLocation id, int amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<Item> item = ItemLookup.find(id);
        if (item.isEmpty()) {
            return CommandUtil.fail(source, "item.unknown", id);
        }
        int left = amount;
        int stackSize = new ItemStack(item.get()).getMaxStackSize();
        while (left > 0) {
            int count = Math.min(left, stackSize);
            addOrDrop(player, new ItemStack(item.get(), count));
            left -= count;
        }
        return CommandUtil.ok(source, "item.given", id, amount);
    }

    private static int itemInfo(CommandSourceStack source, Item item) {
        ItemStack stack = new ItemStack(item);
        return CommandUtil.ok(source, "item.info", ItemLookup.id(item), stack.getMaxStackSize(), stack.getMaxDamage());
    }

    private static int rename(CommandSourceStack source, String name) throws CommandSyntaxException {
        ItemStack stack = held(source);
        if (stack == null) {
            return 0;
        }
        ItemData.setName(stack, name == null ? null : Texts.colored(name));
        return CommandUtil.ok(source, name == null ? "itemname.reset" : "itemname.done");
    }

    private static int loreAdd(CommandSourceStack source, String text) throws CommandSyntaxException {
        ItemStack stack = held(source);
        if (stack == null) {
            return 0;
        }
        List<Component> lore = ItemData.lore(stack);
        lore.add(Texts.colored(text));
        ItemData.setLore(stack, lore);
        return CommandUtil.ok(source, "itemlore.done");
    }

    private static int loreSet(CommandSourceStack source, int line, String text) throws CommandSyntaxException {
        ItemStack stack = held(source);
        if (stack == null) {
            return 0;
        }
        List<Component> lore = ItemData.lore(stack);
        if (line > lore.size()) {
            return CommandUtil.fail(source, "itemlore.bad-line", line);
        }
        lore.set(line - 1, Texts.colored(text));
        ItemData.setLore(stack, lore);
        return CommandUtil.ok(source, "itemlore.done");
    }

    private static int loreClear(CommandSourceStack source) throws CommandSyntaxException {
        ItemStack stack = held(source);
        if (stack == null) {
            return 0;
        }
        ItemData.setLore(stack, List.of());
        return CommandUtil.ok(source, "itemlore.done");
    }

    private static int expShow(CommandSourceStack source, ServerPlayer target) {
        return CommandUtil.ok(source, "exp.show", target.getGameProfile().getName(), target.experienceLevel, target.totalExperience);
    }

    private static int expSet(CommandSourceStack source, ServerPlayer target, int amount, boolean add) {
        if (add) {
            target.giveExperiencePoints(amount);
        } else {
            target.setExperienceLevels(0);
            target.setExperiencePoints(0);
            target.totalExperience = 0;
            target.giveExperiencePoints(amount);
        }
        return CommandUtil.ok(source, "exp.done", target.getGameProfile().getName());
    }

    private static int skull(CommandSourceStack source, String owner, ServerPlayer receiver) throws CommandSyntaxException {
        ServerPlayer target = receiver != null ? receiver : source.getPlayerOrException();
        addOrDrop(target, ItemData.playerHead(owner));
        return CommandUtil.ok(source, "skull.done", owner);
    }

    /** items.condense: nine of an item become one block, across the main inventory. */
    private static int condense(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Inventory inventory = player.getInventory();
        int used = 0;
        for (Map.Entry<String, String> rule : ConfigManager.get().items.condense.entrySet()) {
            Optional<Item> from = ItemLookup.find(Dimensions.location(rule.getKey()));
            Optional<Item> to = ItemLookup.find(Dimensions.location(rule.getValue()));
            if (from.isEmpty() || to.isEmpty()) {
                continue;
            }
            int total = 0;
            for (ItemStack stack : inventory.items) {
                if (stack.getItem() == from.get() && !stack.isDamaged()) {
                    total += stack.getCount();
                }
            }
            int blocks = total / CONDENSE_RATIO;
            int remove = blocks * CONDENSE_RATIO;
            used += remove;
            for (ItemStack stack : new ArrayList<>(inventory.items)) {
                if (remove > 0 && stack.getItem() == from.get()) {
                    int take = Math.min(stack.getCount(), remove);
                    stack.shrink(take);
                    remove -= take;
                }
            }
            int stackSize = new ItemStack(to.get()).getMaxStackSize();
            while (blocks > 0) {
                int count = Math.min(blocks, stackSize);
                addOrDrop(player, new ItemStack(to.get(), count));
                blocks -= count;
            }
        }
        return used == 0 ? CommandUtil.fail(source, "condense.nothing") : CommandUtil.ok(source, "condense.done", used);
    }

    private static int editSign(CommandSourceStack source, int line, String text) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = Players.level(player);
        HitResult hit = player.pick(LOOK_RANGE, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return CommandUtil.fail(source, "editsign.not-sign");
        }
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
            return CommandUtil.fail(source, "editsign.not-sign");
        }
        Signs.setLine(sign, line - 1, Texts.colored(text));
        sign.setChanged();
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        return CommandUtil.ok(source, "editsign.done");
    }

    private static int unlimitedList(CommandSourceStack source, ServerPlayer player) {
        var items = EssData.get(source.getServer()).player(player.getUUID()).map(PlayerRecord::unlimited).orElse(Set.of());
        return CommandUtil.ok(source, "unlimited.list", String.join(", ", items));
    }

    private static int unlimited(CommandSourceStack source, ResourceLocation id, ServerPlayer target) {
        if (ItemLookup.find(id).isEmpty()) {
            return CommandUtil.fail(source, "item.unknown", id);
        }
        boolean on = EssData.get(source.getServer()).toggleUnlimited(target.getUUID(), id.toString());
        return CommandUtil.ok(source, on ? "unlimited.on" : "unlimited.off", id, target.getGameProfile().getName());
    }

    private static int powerTool(CommandSourceStack source, String command) throws CommandSyntaxException {
        ItemStack stack = held(source);
        if (stack == null) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        String id = ItemLookup.id(stack.getItem()).toString();
        String line = command == null ? null : command.startsWith("/") ? command.substring(1) : command;
        EssData.get(source.getServer()).setPowerTool(player.getUUID(), id, line);
        return line == null ? CommandUtil.ok(source, "powertool.removed", id) : CommandUtil.ok(source, "powertool.set", id, line);
    }

    private static int powerToolList(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Map<String, String> tools = EssData.get(source.getServer()).player(player.getUUID()).map(PlayerRecord::powerTools).orElse(Map.of());
        if (tools.isEmpty()) {
            return CommandUtil.ok(source, "powertool.none");
        }
        List<String> lines = new ArrayList<>();
        tools.forEach((item, command) -> lines.add(item + " -> /" + command));
        return CommandUtil.ok(source, "powertool.list", String.join(", ", lines));
    }
}
