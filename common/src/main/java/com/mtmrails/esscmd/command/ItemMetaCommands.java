package com.mtmrails.esscmd.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mtmrails.esscmd.compat.Books;
import com.mtmrails.esscmd.compat.Fireworks;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.compat.Potions;
import com.mtmrails.esscmd.message.Messages;
import com.mtmrails.esscmd.permission.PermissionNodes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * {@code /book}, {@code /firework} and {@code /potion}: edit the written book, firework rocket
 * or potion in the main hand. Options use Essentials' {@code key:value} syntax.
 */
public final class ItemMetaCommands {
    private static final int MAX_POWER = 3;
    private static final int MAX_FIRE = 10;
    private static final int MAX_EFFECT_LEVEL = 255;
    private static final int MAX_EFFECT_SECONDS = 1_000_000;
    private static final int DEFAULT_EFFECT_SECONDS = 60;
    private static final int TICKS_PER_SECOND = 20;

    private static final Map<String, Integer> SHAPES = Map.of(
            "ball", 0, "small_ball", 0, "small", 0,
            "large", 1, "ball_large", 1, "large_ball", 1,
            "star", 2, "creeper", 3, "burst", 4);
    private static final List<String> SHAPE_NAMES = List.of("small_ball", "large_ball", "star", "creeper", "burst");
    private static final List<String> FIREWORK_EFFECTS = List.of("trail", "twinkle");

    private static final SuggestionProvider<CommandSourceStack> FIREWORK_OPTIONS = (ctx, builder) ->
            suggestOptions(builder, List.of("color:", "fade:", "shape:", "effect:"), (key, out) -> {
                switch (key) {
                    case "color", "colour", "fade" -> {
                        for (DyeColor color : DyeColor.values()) {
                            out.add(color.getName());
                        }
                    }
                    case "shape", "type" -> out.addAll(SHAPE_NAMES);
                    case "effect" -> out.addAll(FIREWORK_EFFECTS);
                    default -> { }
                }
            });

    private static final SuggestionProvider<CommandSourceStack> POTION_OPTIONS = (ctx, builder) ->
            suggestOptions(builder, List.of("effect:", "power:", "duration:"), (key, out) -> {
                if (key.equals("effect")) {
                    Potions.effectIds().forEach(id -> out.add(id.toString()));
                }
            });

    private ItemMetaCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("book")
                .requires(PermissionNodes.BOOK.require())
                .executes(ctx -> book(ctx.getSource()))
                .then(Commands.literal("title")
                        .requires(PermissionNodes.BOOK_TITLE.require())
                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                .executes(ctx -> bookTitle(ctx.getSource(), StringArgumentType.getString(ctx, "title")))))
                .then(Commands.literal("author")
                        .requires(PermissionNodes.BOOK_AUTHOR.require())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> bookAuthor(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))));

        dispatcher.register(Commands.literal("firework")
                .requires(PermissionNodes.FIREWORK.require())
                .then(Commands.literal("clear").executes(ctx -> fireworkClear(ctx.getSource())))
                .then(Commands.literal("power")
                        .executes(ctx -> fireworkPower(ctx.getSource(), -1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, MAX_POWER))
                                .executes(ctx -> fireworkPower(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount")))))
                .then(Commands.literal("fire")
                        .requires(PermissionNodes.FIREWORK_FIRE.require())
                        .executes(ctx -> fireworkFire(ctx.getSource(), 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, MAX_FIRE))
                                .executes(ctx -> fireworkFire(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount")))))
                .then(Commands.argument("options", StringArgumentType.greedyString()).suggests(FIREWORK_OPTIONS)
                        .executes(ctx -> fireworkAdd(ctx.getSource(), StringArgumentType.getString(ctx, "options")))));

        LiteralCommandNode<CommandSourceStack> potion = dispatcher.register(Commands.literal("potion")
                .requires(PermissionNodes.POTION.require())
                .then(Commands.literal("clear").executes(ctx -> potionClear(ctx.getSource())))
                .then(Commands.literal("apply")
                        .requires(PermissionNodes.POTION_APPLY.require())
                        .executes(ctx -> potionApply(ctx.getSource())))
                .then(Commands.argument("options", StringArgumentType.greedyString()).suggests(POTION_OPTIONS)
                        .executes(ctx -> potionAdd(ctx.getSource(), StringArgumentType.getString(ctx, "options")))));
        dispatcher.register(Commands.literal("elixer").requires(potion.getRequirement()).redirect(potion));
    }

    // ---- /book ----

    private static int book(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (Books.isWritten(stack)) {
            if (!mayEdit(source, player, stack)) {
                return CommandUtil.fail(source, "book.not-author");
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, Books.unseal(stack));
            return CommandUtil.ok(source, "book.unsealed");
        }
        if (Books.isWritable(stack)) {
            String title = Messages.raw(source, "book.default-title");
            if (title.length() > Books.TITLE_MAX_LENGTH) {
                title = title.substring(0, Books.TITLE_MAX_LENGTH);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, Books.seal(stack, title, Players.name(player)));
            return CommandUtil.ok(source, "book.sealed");
        }
        return CommandUtil.fail(source, "book.not-book");
    }

    private static int bookTitle(CommandSourceStack source, String title) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = writtenBook(source, player);
        if (stack == null) {
            return 0;
        }
        if (title.length() > Books.TITLE_MAX_LENGTH) {
            return CommandUtil.fail(source, "book.title-too-long", Books.TITLE_MAX_LENGTH);
        }
        Books.setTitle(stack, title);
        return CommandUtil.ok(source, "book.title-set", title);
    }

    private static int bookAuthor(CommandSourceStack source, String author) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = writtenBook(source, player);
        if (stack == null) {
            return 0;
        }
        Books.setAuthor(stack, author);
        return CommandUtil.ok(source, "book.author-set", author);
    }

    /** The held written book if the player may edit it, else null after telling them why. */
    private static ItemStack writtenBook(CommandSourceStack source, ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!Books.isWritten(stack)) {
            CommandUtil.fail(source, "book.not-written");
            return null;
        }
        if (!mayEdit(source, player, stack)) {
            CommandUtil.fail(source, "book.not-author");
            return null;
        }
        return stack;
    }

    private static boolean mayEdit(CommandSourceStack source, ServerPlayer player, ItemStack book) {
        return Books.author(book).equalsIgnoreCase(Players.name(player)) || PermissionNodes.BOOK_OTHERS.check(source);
    }

    // ---- /firework ----

    private static ItemStack rocket(CommandSourceStack source) throws CommandSyntaxException {
        ItemStack stack = source.getPlayerOrException().getMainHandItem();
        if (!stack.is(Items.FIREWORK_ROCKET)) {
            CommandUtil.fail(source, "firework.not-rocket");
            return null;
        }
        return stack;
    }

    private static int fireworkClear(CommandSourceStack source) throws CommandSyntaxException {
        ItemStack stack = rocket(source);
        if (stack == null) {
            return 0;
        }
        Fireworks.set(stack, Fireworks.power(stack), List.of());
        return CommandUtil.ok(source, "firework.cleared");
    }

    private static int fireworkPower(CommandSourceStack source, int power) throws CommandSyntaxException {
        ItemStack stack = rocket(source);
        if (stack == null) {
            return 0;
        }
        if (power < 0) {
            return CommandUtil.ok(source, "firework.power", Fireworks.power(stack));
        }
        Fireworks.set(stack, power, Fireworks.explosions(stack));
        return CommandUtil.ok(source, "firework.power-set", power);
    }

    private static int fireworkFire(CommandSourceStack source, int amount) throws CommandSyntaxException {
        ItemStack stack = rocket(source);
        if (stack == null) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        for (int i = 0; i < amount; i++) {
            Fireworks.launch(Players.level(player), player.getX(), player.getY(), player.getZ(), stack);
        }
        return CommandUtil.ok(source, "firework.fired", amount);
    }

    private static int fireworkAdd(CommandSourceStack source, String options) throws CommandSyntaxException {
        ItemStack stack = rocket(source);
        if (stack == null) {
            return 0;
        }
        List<Integer> colors = new ArrayList<>();
        List<Integer> fades = new ArrayList<>();
        int shape = 0;
        boolean trail = false;
        boolean twinkle = false;
        for (String[] option : options(options)) {
            String key = option[0];
            String value = option[1];
            switch (key) {
                case "color", "colour", "fade" -> {
                    List<Integer> target = key.equals("fade") ? fades : colors;
                    for (String name : value.split(",")) {
                        Integer rgb = color(name);
                        if (rgb == null) {
                            return CommandUtil.fail(source, "firework.bad-color", name, colorNames());
                        }
                        target.add(rgb);
                    }
                }
                case "shape", "type" -> {
                    Integer id = SHAPES.get(value);
                    if (id == null) {
                        return CommandUtil.fail(source, "firework.bad-shape", value, String.join(", ", SHAPE_NAMES));
                    }
                    shape = id;
                }
                case "effect" -> {
                    for (String effect : value.split(",")) {
                        switch (effect) {
                            case "trail" -> trail = true;
                            case "twinkle", "flicker" -> twinkle = true;
                            default -> {
                                return CommandUtil.fail(source, "firework.bad-effect", effect, String.join(", ", FIREWORK_EFFECTS));
                            }
                        }
                    }
                }
                default -> {
                    return CommandUtil.fail(source, "firework.bad-option", key);
                }
            }
        }
        if (colors.isEmpty()) {
            return CommandUtil.fail(source, "firework.no-color");
        }
        List<Fireworks.Explosion> explosions = Fireworks.explosions(stack);
        if (explosions.size() >= Fireworks.MAX_EXPLOSIONS) {
            return CommandUtil.fail(source, "firework.too-many", Fireworks.MAX_EXPLOSIONS);
        }
        explosions.add(new Fireworks.Explosion(shape, colors, fades, trail, twinkle));
        Fireworks.set(stack, Fireworks.power(stack), explosions);
        return CommandUtil.ok(source, "firework.added", explosions.size());
    }

    /** A dye colour name such as {@code light_blue}, or {@code #RRGGBB}. */
    private static Integer color(String name) {
        if (name.startsWith("#") && name.length() == 7) {
            try {
                return Integer.parseInt(name.substring(1), 16);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        DyeColor dye = DyeColor.byName(name, null);
        return dye == null ? null : dye.getFireworkColor();
    }

    private static String colorNames() {
        List<String> names = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            names.add(color.getName());
        }
        return String.join(", ", names) + ", #RRGGBB";
    }

    // ---- /potion ----

    private static ItemStack potionItem(CommandSourceStack source) throws CommandSyntaxException {
        ItemStack stack = source.getPlayerOrException().getMainHandItem();
        if (!Potions.isPotion(stack)) {
            CommandUtil.fail(source, "potion.not-potion");
            return null;
        }
        return stack;
    }

    private static int potionClear(CommandSourceStack source) throws CommandSyntaxException {
        ItemStack stack = potionItem(source);
        if (stack == null) {
            return 0;
        }
        Potions.setCustomEffects(stack, List.of());
        return CommandUtil.ok(source, "potion.cleared");
    }

    private static int potionApply(CommandSourceStack source) throws CommandSyntaxException {
        ItemStack stack = potionItem(source);
        if (stack == null) {
            return 0;
        }
        List<MobEffectInstance> effects = Potions.allEffects(stack);
        if (effects.isEmpty()) {
            return CommandUtil.fail(source, "potion.no-effects");
        }
        ServerPlayer player = source.getPlayerOrException();
        effects.forEach(effect -> player.addEffect(new MobEffectInstance(effect)));
        return CommandUtil.ok(source, "potion.applied", effects.size());
    }

    private static int potionAdd(CommandSourceStack source, String options) throws CommandSyntaxException {
        ItemStack stack = potionItem(source);
        if (stack == null) {
            return 0;
        }
        ResourceLocation id = null;
        int level = 1;
        int seconds = DEFAULT_EFFECT_SECONDS;
        for (String[] option : options(options)) {
            String key = option[0];
            String value = option[1];
            switch (key) {
                case "effect" -> {
                    id = ResourceLocation.tryParse(value);
                    if (id == null) {
                        return CommandUtil.fail(source, "potion.bad-effect", value);
                    }
                }
                case "power" -> {
                    Integer parsed = number(value, 1, MAX_EFFECT_LEVEL);
                    if (parsed == null) {
                        return CommandUtil.fail(source, "potion.bad-number", key, value, 1, MAX_EFFECT_LEVEL);
                    }
                    level = parsed;
                }
                case "duration" -> {
                    Integer parsed = number(value, 1, MAX_EFFECT_SECONDS);
                    if (parsed == null) {
                        return CommandUtil.fail(source, "potion.bad-number", key, value, 1, MAX_EFFECT_SECONDS);
                    }
                    seconds = parsed;
                }
                default -> {
                    return CommandUtil.fail(source, "potion.bad-option", key);
                }
            }
        }
        if (id == null) {
            return CommandUtil.fail(source, "potion.no-effect");
        }
        var created = Potions.effect(id, seconds * TICKS_PER_SECOND, level - 1);
        if (created.isEmpty()) {
            return CommandUtil.fail(source, "potion.bad-effect", id);
        }
        MobEffectInstance effect = created.get();
        ResourceLocation effectId = Potions.id(effect);
        List<MobEffectInstance> effects = Potions.customEffects(stack);
        effects.removeIf(existing -> Potions.id(existing).equals(effectId));
        effects.add(effect);
        Potions.setCustomEffects(stack, effects);
        Component name = Potions.name(effect).copy().withStyle(ChatFormatting.GOLD);
        return CommandUtil.ok(source, "potion.added", name, level, seconds);
    }

    // ---- key:value options ----

    /** {@code a:b c:d} as lower-cased {@code [key, value]} pairs; a token without a colon has an empty value. */
    private static List<String[]> options(String input) {
        List<String[]> result = new ArrayList<>();
        for (String token : input.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            int colon = token.indexOf(':');
            String key = (colon < 0 ? token : token.substring(0, colon)).toLowerCase(Locale.ROOT);
            String value = colon < 0 ? "" : token.substring(colon + 1).toLowerCase(Locale.ROOT);
            // effect:minecraft:speed keeps its namespace
            result.add(new String[] {key, value});
        }
        return result;
    }

    private static Integer number(String value, int min, int max) {
        try {
            int n = Integer.parseInt(value);
            return n < min || n > max ? null : n;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private interface ValueSuggester {
        void suggest(String key, List<String> out);
    }

    /** Suggests keys for the token being typed, or values once it has {@code key:}. */
    private static CompletableFuture<Suggestions> suggestOptions(SuggestionsBuilder builder, List<String> keys, ValueSuggester values) {
        String remaining = builder.getRemaining();
        int tokenStart = remaining.lastIndexOf(' ') + 1;
        String token = remaining.substring(tokenStart).toLowerCase(Locale.ROOT);
        SuggestionsBuilder at = builder.createOffset(builder.getStart() + tokenStart);
        int colon = token.indexOf(':');
        if (colon < 0) {
            keys.stream().filter(key -> key.startsWith(token)).forEach(at::suggest);
            return at.buildFuture();
        }
        String key = token.substring(0, colon);
        String typed = token.substring(colon + 1);
        int lastComma = typed.lastIndexOf(',');
        String prefix = token.substring(0, colon + 1 + lastComma + 1);
        String part = typed.substring(lastComma + 1);
        List<String> candidates = new ArrayList<>();
        values.suggest(key, candidates);
        for (String candidate : candidates) {
            if (candidate.startsWith(part) || candidate.startsWith("minecraft:" + part)) {
                at.suggest(prefix + candidate);
            }
        }
        return at.buildFuture();
    }
}
