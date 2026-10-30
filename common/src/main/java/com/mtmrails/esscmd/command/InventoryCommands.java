package com.mtmrails.esscmd.command;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mtmrails.esscmd.compat.ItemLookup;
import com.mtmrails.esscmd.compat.Texts;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.menu.InvseeMenu;
import com.mtmrails.esscmd.menu.PortableMenus;
import com.mtmrails.esscmd.message.Messages;
import com.mtmrails.esscmd.permission.PermissionNode;
import com.mtmrails.esscmd.permission.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Inventory tools and portable blocks: {@code /enderchest}, {@code /invsee}, {@code /hat},
 * {@code /repair}, {@code /more}, {@code /clearinventory}, {@code /clearinventoryconfirmtoggle},
 * {@code /disposal}, {@code /workbench}, {@code /anvil}, {@code /cartographytable},
 * {@code /grindstone}, {@code /loom}, {@code /smithingtable}, {@code /stonecutter}.
 */
public final class InventoryCommands {
    /** Main inventory only; {@code **} also covers armor and offhand. */
    private static final int MAIN_SLOTS = 36;
    private static final int CONFIRM_TICKS = 30 * 20;

    private interface PortableMenu {
        AbstractContainerMenu create(int id, Inventory inventory, ServerPlayer player);
    }

    /** Whose inventory /clearinventory works on; {@code *} as the first argument means everyone. */
    private enum Scope { SELF, TARGETS, ALL }

    /** Pending /clearinventory confirmations: player -> command text and expiry tick. */
    private record Pending(String input, int expiresTick) {}

    private static final Map<UUID, Pending> PENDING_CLEAR = new HashMap<>();

    private InventoryCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        portable(dispatcher, "workbench", List.of("wb", "craft", "wbench"), PermissionNodes.WORKBENCH,
                "container.crafting", PortableMenus::workbench);
        portable(dispatcher, "anvil", List.of(), PermissionNodes.ANVIL, "container.repair", PortableMenus::anvil);
        portable(dispatcher, "cartographytable", List.of("carttable"), PermissionNodes.CARTOGRAPHYTABLE,
                "container.cartography_table", PortableMenus::cartographyTable);
        portable(dispatcher, "grindstone", List.of(), PermissionNodes.GRINDSTONE,
                "container.grindstone_title", PortableMenus::grindstone);
        portable(dispatcher, "loom", List.of(), PermissionNodes.LOOM, "container.loom", PortableMenus::loom);
        portable(dispatcher, "smithingtable", List.of("smithtable"), PermissionNodes.SMITHINGTABLE,
                "container.upgrade", PortableMenus::smithingTable);
        portable(dispatcher, "stonecutter", List.of(), PermissionNodes.STONECUTTER,
                "container.stonecutter", PortableMenus::stonecutter);

        alias(dispatcher, dispatcher.register(Commands.literal("enderchest")
                .requires(PermissionNodes.ENDERCHEST.require())
                .executes(ctx -> enderChest(ctx.getSource().getPlayerOrException(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(PermissionNodes.ENDERCHEST_OTHERS.require())
                        .executes(ctx -> enderChest(ctx.getSource().getPlayerOrException(),
                                EntityArgument.getPlayer(ctx, "player"))))), List.of("ec", "echest"));
        dispatcher.register(Commands.literal("invsee")
                .requires(PermissionNodes.INVSEE.require())
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> invsee(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
        alias(dispatcher, dispatcher.register(Commands.literal("hat")
                .requires(PermissionNodes.HAT.require())
                .executes(ctx -> hat(ctx.getSource()))
                .then(Commands.literal("remove").executes(ctx -> hatRemove(ctx.getSource())))), List.of("head"));
        alias(dispatcher, dispatcher.register(Commands.literal("repair")
                .requires(PermissionNodes.REPAIR.require())
                .executes(ctx -> repairHand(ctx.getSource()))
                .then(Commands.literal("hand").executes(ctx -> repairHand(ctx.getSource())))
                .then(Commands.literal("all")
                        .requires(PermissionNodes.REPAIR_ALL.require())
                        .executes(ctx -> repairAll(ctx.getSource())))), List.of("fix"));
        dispatcher.register(Commands.literal("more")
                .requires(PermissionNodes.MORE.require())
                .executes(ctx -> {
                    int size = ConfigManager.get().items.defaultStackSize;
                    return more(ctx.getSource(), size > 0 ? size : Integer.MAX_VALUE);
                })
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> more(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount")))));
        alias(dispatcher, dispatcher.register(Commands.literal("disposal")
                .requires(PermissionNodes.DISPOSAL.require())
                .executes(ctx -> disposal(ctx.getSource()))), List.of("trash"));
        registerClear(dispatcher);
        dispatcher.register(Commands.literal("clearinventoryconfirmtoggle")
                .requires(PermissionNodes.CLEARINVENTORY.require())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    EssData data = EssData.get(player.server);
                    boolean on = !data.clearConfirm(player.getUUID());
                    data.setClearConfirm(player.getUUID(), on);
                    return CommandUtil.ok(ctx.getSource(), on ? "clear.confirm-on" : "clear.confirm-off");
                }));
    }

    /** Aliases with no required argument need {@code executes} copied, see EssentialsCommand. */
    private static void alias(CommandDispatcher<CommandSourceStack> dispatcher, LiteralCommandNode<CommandSourceStack> target,
                              List<String> names) {
        for (String name : names) {
            dispatcher.register(Commands.literal(name)
                    .requires(target.getRequirement())
                    .executes(target.getCommand())
                    .redirect(target));
        }
    }

    private static void portable(CommandDispatcher<CommandSourceStack> dispatcher, String name, List<String> aliases,
                                 PermissionNode node, String titleKey, PortableMenu menu) {
        alias(dispatcher, dispatcher.register(Commands.literal(name)
                .requires(node.require())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    open(player, (id, inventory, viewer) -> menu.create(id, inventory, player),
                            Texts.vanillaTranslatable(titleKey));
                    return 1;
                })), aliases);
    }

    private static void open(ServerPlayer player, MenuConstructor menu, Component title) {
        player.openMenu(new SimpleMenuProvider(menu, title));
    }

    private static int enderChest(ServerPlayer viewer, ServerPlayer owner) {
        Container chest = owner.getEnderChestInventory();
        Component title = viewer == owner ? Texts.vanillaTranslatable("container.enderchest")
                : Messages.info(viewer, "enderchest.title", owner.getGameProfile().getName());
        open(viewer, (id, inventory, player) -> new ChestMenu(MenuType.GENERIC_9x3, id, inventory, chest, 3) {
            // Not the container's check: it follows the owner's open ender chest block.
            @Override
            public boolean stillValid(Player ignored) {
                return !owner.hasDisconnected();
            }
        }, title);
        return 1;
    }

    private static int invsee(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer viewer = source.getPlayerOrException();
        if (viewer == target) {
            return CommandUtil.fail(source, "invsee.self");
        }
        boolean editable = PermissionNodes.INVSEE_MODIFY.check(source);
        open(viewer, (id, inventory, player) -> new InvseeMenu(id, inventory, target, editable),
                Messages.info(viewer, "invsee.title", target.getGameProfile().getName()));
        return 1;
    }

    private static int hat(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty()) {
            return CommandUtil.fail(source, "hat.empty-hand");
        }
        // Swap the whole stack, like clicking it into the helmet slot.
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        player.setItemSlot(EquipmentSlot.HEAD, hand.copy());
        player.setItemSlot(EquipmentSlot.MAINHAND, head.copy());
        return CommandUtil.ok(source, "hat.done");
    }

    private static int hatRemove(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) {
            return CommandUtil.fail(source, "hat.none");
        }
        if (player.getInventory().getFreeSlot() < 0) {
            return CommandUtil.fail(source, "hat.no-room");
        }
        player.getInventory().add(head.copy());
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        return CommandUtil.ok(source, "hat.removed");
    }

    private static boolean canRepairEnchantment(ItemStack stack) {
        return ConfigManager.get().items.repairEnchanted || !stack.isEnchanted();
    }

    private static int repairHand(CommandSourceStack source) throws CommandSyntaxException {
        ItemStack stack = source.getPlayerOrException().getMainHandItem();
        if (!stack.isDamageableItem()) {
            return CommandUtil.fail(source, "repair.invalid");
        }
        if (!canRepairEnchantment(stack)) {
            return CommandUtil.fail(source, "repair.enchanted");
        }
        stack.setDamageValue(0);
        return CommandUtil.ok(source, "repair.done", 1);
    }

    private static int repairAll(CommandSourceStack source) throws CommandSyntaxException {
        Inventory inventory = source.getPlayerOrException().getInventory();
        int repaired = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isDamageableItem() && stack.isDamaged() && canRepairEnchantment(stack)) {
                stack.setDamageValue(0);
                repaired++;
            }
        }
        return CommandUtil.ok(source, "repair.done", repaired);
    }

    private static int more(CommandSourceStack source, int amount) throws CommandSyntaxException {
        ItemStack stack = source.getPlayerOrException().getMainHandItem();
        if (stack.isEmpty()) {
            return CommandUtil.fail(source, "hat.empty-hand");
        }
        stack.setCount(Math.min(amount, stack.getMaxStackSize()));
        return CommandUtil.ok(source, "more.done", stack.getCount());
    }

    private static int disposal(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        // A throwaway container: whatever is left in it disappears on close.
        open(player, (id, inventory, viewer) -> ChestMenu.sixRows(id, inventory, new SimpleContainer(54)),
                Messages.info(player, "disposal.title"));
        return 1;
    }

    // /clearinventory [player|*] [item|*|**] [amount]

    private static void registerClear(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(Commands.literal("clearinventory")
                .requires(PermissionNodes.CLEARINVENTORY.require())
                .executes(ctx -> clear(ctx, Scope.SELF, null, false, Integer.MAX_VALUE))
                .then(Commands.literal("**").executes(ctx -> clear(ctx, Scope.SELF, null, true, Integer.MAX_VALUE)))
                .then(itemArguments(Commands.literal("*")
                        .requires(PermissionNodes.CLEARINVENTORY_OTHERS.require()), Scope.ALL))
                .then(itemArguments(Commands.argument("targets", EntityArgument.players())
                        .requires(PermissionNodes.CLEARINVENTORY_OTHERS.require()), Scope.TARGETS)));
        alias(dispatcher, node, List.of("ci", "clean"));
    }

    /** The {@code [item|*|**] [amount]} tail, for your own inventory or {@code targets}. */
    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T itemArguments(T builder, Scope scope) {
        return builder
                .executes(ctx -> clear(ctx, scope, null, false, Integer.MAX_VALUE))
                .then(Commands.literal("*").executes(ctx -> clear(ctx, scope, null, false, Integer.MAX_VALUE)))
                .then(Commands.literal("**").executes(ctx -> clear(ctx, scope, null, true, Integer.MAX_VALUE)))
                .then(Commands.argument("item", ResourceLocationArgument.id())
                        .suggests((ctx, suggestions) -> SharedSuggestionProvider.suggestResource(ItemLookup.ids(), suggestions))
                        .executes(ctx -> clear(ctx, scope, item(ctx), true, Integer.MAX_VALUE))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> clear(ctx, scope, item(ctx), true,
                                        IntegerArgumentType.getInteger(ctx, "amount")))));
    }

    private static Item item(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "item");
        Optional<Item> item = ItemLookup.find(id);
        if (item.isEmpty()) {
            throw new SimpleCommandExceptionType(
                    Messages.error(ctx.getSource(), "clear.unknown-item", id)).create();
        }
        return item.get();
    }

    private static int clear(CommandContext<CommandSourceStack> ctx, Scope scope, Item item, boolean everything, int amount)
            throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Collection<ServerPlayer> targets = switch (scope) {
            case SELF -> List.of(source.getPlayerOrException());
            case TARGETS -> EntityArgument.getPlayers(ctx, "targets");
            case ALL -> source.getServer().getPlayerList().getPlayers();
        };
        if (source.getEntity() instanceof ServerPlayer player
                && EssData.get(source.getServer()).clearConfirm(player.getUUID()) && !confirmed(player, ctx.getInput())) {
            return CommandUtil.ok(source, "clear.confirm");
        }
        int removed = 0;
        for (ServerPlayer target : targets) {
            removed += removeItems(target.getInventory(), item, everything, amount);
            target.containerMenu.broadcastChanges();
        }
        return CommandUtil.ok(source, "clear.done", removed, targets.size());
    }

    /** True when the same command was given within the confirmation window; otherwise arms it. */
    private static boolean confirmed(ServerPlayer player, String input) {
        int now = player.server.getTickCount();
        Pending pending = PENDING_CLEAR.remove(player.getUUID());
        if (pending != null && pending.input().equals(input) && now < pending.expiresTick()) {
            return true;
        }
        PENDING_CLEAR.put(player.getUUID(), new Pending(input, now + CONFIRM_TICKS));
        return false;
    }

    /** {@code item == null} matches everything. {@code everything} includes armor and offhand. */
    private static int removeItems(Inventory inventory, Item item, boolean everything, int amount) {
        int slots = everything ? inventory.getContainerSize() : MAIN_SLOTS;
        int left = amount;
        int removed = 0;
        for (int i = 0; i < slots && left > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || (item != null && stack.getItem() != item)) {
                continue;
            }
            int take = Math.min(stack.getCount(), left);
            stack.shrink(take);
            left -= take;
            removed += take;
        }
        return removed;
    }
}
