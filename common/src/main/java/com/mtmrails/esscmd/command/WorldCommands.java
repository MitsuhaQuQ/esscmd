package com.mtmrails.esscmd.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mtmrails.esscmd.compat.ItemLookup;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.compat.Spawners;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.permission.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /thunder}, {@code /lightning}, {@code /burn}, {@code /ice}, {@code /remove},
 * {@code /spawner}, {@code /spawnmob}, {@code /tree}, {@code /bigtree}, {@code /break}.
 */
public final class WorldCommands {
    private static final int LOOK_RANGE = 100;
    private static final int TICKS_PER_SECOND = 20;

    /** Tree names accepted by /tree. */
    private static final Map<String, Block> TREES = Map.of("tree", Blocks.OAK_SAPLING, "oak", Blocks.OAK_SAPLING,
            "birch", Blocks.BIRCH_SAPLING, "redwood", Blocks.SPRUCE_SAPLING, "spruce", Blocks.SPRUCE_SAPLING,
            "jungle", Blocks.JUNGLE_SAPLING, "acacia", Blocks.ACACIA_SAPLING, "darkoak", Blocks.DARK_OAK_SAPLING);
    /** /bigtree: four saplings in a square. */
    private static final Map<String, Block> BIG_TREES = Map.of("redwood", Blocks.SPRUCE_SAPLING,
            "jungle", Blocks.JUNGLE_SAPLING, "darkoak", Blocks.DARK_OAK_SAPLING);

    private static final Map<String, Predicate<Entity>> REMOVABLE = Map.of(
            "drops", entity -> entity instanceof ItemEntity,
            "arrows", entity -> entity instanceof AbstractArrow,
            "xp", entity -> entity instanceof ExperienceOrb,
            "monsters", entity -> entity instanceof Enemy,
            "animals", entity -> entity instanceof Animal);

    private WorldCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("thunder")
                .requires(PermissionNodes.THUNDER.require())
                .then(Commands.argument("on", BoolArgumentType.bool())
                        .executes(ctx -> thunder(ctx.getSource(), BoolArgumentType.getBool(ctx, "on"), 6000))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                .executes(ctx -> thunder(ctx.getSource(), BoolArgumentType.getBool(ctx, "on"),
                                        IntegerArgumentType.getInteger(ctx, "seconds") * TICKS_PER_SECOND)))));
        dispatcher.register(Commands.literal("lightning")
                .requires(PermissionNodes.LIGHTNING.require())
                .executes(ctx -> lightningAtLook(ctx.getSource()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(PermissionNodes.LIGHTNING_OTHERS.require())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            strike(Players.level(target), target.position());
                            return CommandUtil.ok(ctx.getSource(), "lightning.done", target.getGameProfile().getName());
                        })));
        dispatcher.register(Commands.literal("burn")
                .requires(PermissionNodes.BURN.require())
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                    target.setRemainingFireTicks(seconds * TICKS_PER_SECOND);
                                    return CommandUtil.ok(ctx.getSource(), "burn.done", target.getGameProfile().getName(), seconds);
                                }))));
        dispatcher.register(Commands.literal("ice")
                .requires(PermissionNodes.ICE.require())
                .executes(ctx -> ice(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(PermissionNodes.ICE_OTHERS.require())
                        .executes(ctx -> ice(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
        dispatcher.register(Commands.literal("remove")
                .requires(PermissionNodes.REMOVE.require())
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(REMOVABLE.keySet(), builder))
                        .executes(ctx -> remove(ctx.getSource(), StringArgumentType.getString(ctx, "type"), -1))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                .executes(ctx -> remove(ctx.getSource(), StringArgumentType.getString(ctx, "type"),
                                        IntegerArgumentType.getInteger(ctx, "radius"))))));
        dispatcher.register(Commands.literal("spawner")
                .requires(PermissionNodes.SPAWNER.require())
                .then(Commands.argument("mob", ResourceLocationArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(ItemLookup.entityIds(), builder))
                        .executes(ctx -> spawner(ctx.getSource(), ResourceLocationArgument.getId(ctx, "mob")))));
        dispatcher.register(Commands.literal("spawnmob")
                .requires(PermissionNodes.SPAWNMOB.require())
                .then(Commands.argument("mob", ResourceLocationArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(ItemLookup.entityIds(), builder))
                        .executes(ctx -> spawnMob(ctx.getSource(), ResourceLocationArgument.getId(ctx, "mob"), 1, null))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> spawnMob(ctx.getSource(), ResourceLocationArgument.getId(ctx, "mob"),
                                        IntegerArgumentType.getInteger(ctx, "amount"), null))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> spawnMob(ctx.getSource(), ResourceLocationArgument.getId(ctx, "mob"),
                                                IntegerArgumentType.getInteger(ctx, "amount"), EntityArgument.getPlayer(ctx, "player")))))));
        dispatcher.register(Commands.literal("tree")
                .requires(PermissionNodes.TREE.require())
                .executes(ctx -> tree(ctx.getSource(), "tree", false))
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(TREES.keySet(), builder))
                        .executes(ctx -> tree(ctx.getSource(), StringArgumentType.getString(ctx, "type"), false))));
        dispatcher.register(Commands.literal("bigtree")
                .requires(PermissionNodes.TREE.require())
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(BIG_TREES.keySet(), builder))
                        .executes(ctx -> tree(ctx.getSource(), StringArgumentType.getString(ctx, "type"), true))));
        dispatcher.register(Commands.literal("break")
                .requires(PermissionNodes.BREAK.require())
                .executes(ctx -> breakBlock(ctx.getSource())));
    }

    private static Optional<BlockPos> lookedAt(ServerPlayer player) {
        HitResult hit = player.pick(LOOK_RANGE, 1.0f, false);
        return hit.getType() == HitResult.Type.BLOCK ? Optional.of(((BlockHitResult) hit).getBlockPos()) : Optional.empty();
    }

    private static int thunder(CommandSourceStack source, boolean on, int ticks) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        level.setWeatherParameters(on ? 0 : ticks, on ? ticks : 0, on, on);
        return CommandUtil.ok(source, on ? "thunder.on" : "thunder.off");
    }

    private static void strike(ServerLevel level, Vec3 position) {
        Entity bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.setPos(position.x, position.y, position.z);
            level.addFreshEntity(bolt);
        }
    }

    private static int lightningAtLook(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<BlockPos> block = lookedAt(player);
        if (block.isEmpty()) {
            return CommandUtil.fail(source, "jump.nothing");
        }
        strike(Players.level(player), Vec3.atBottomCenterOf(block.get().above()));
        return 1;
    }

    private static int ice(CommandSourceStack source, ServerPlayer target) {
        target.setTicksFrozen(target.getTicksRequiredToFreeze());
        return CommandUtil.ok(source, "ice.done", target.getGameProfile().getName());
    }

    private static int remove(CommandSourceStack source, String type, int radius) {
        Predicate<Entity> filter = REMOVABLE.get(type.toLowerCase(Locale.ROOT));
        if (filter == null) {
            return CommandUtil.fail(source, "remove.invalid", String.join(", ", REMOVABLE.keySet()));
        }
        Vec3 center = source.getPosition();
        double maxSqr = radius < 0 ? Double.MAX_VALUE : (double) radius * radius;
        List<Entity> matches = new ArrayList<>();
        for (Entity entity : source.getLevel().getAllEntities()) {
            if (entity != null && filter.test(entity) && entity.position().distanceToSqr(center) <= maxSqr) {
                matches.add(entity);
            }
        }
        matches.forEach(Entity::discard);
        return CommandUtil.ok(source, "remove.done", matches.size());
    }

    private static int spawner(CommandSourceStack source, ResourceLocation id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = Players.level(player);
        Optional<EntityType<?>> type = ItemLookup.entity(id);
        if (type.isEmpty()) {
            return CommandUtil.fail(source, "mob.unknown", id);
        }
        Optional<BlockPos> block = lookedAt(player);
        if (block.isEmpty() || !(level.getBlockEntity(block.get()) instanceof SpawnerBlockEntity spawner)) {
            return CommandUtil.fail(source, "spawner.not-spawner");
        }
        Spawners.setMob(spawner, type.get(), level, block.get());
        spawner.setChanged();
        BlockState state = level.getBlockState(block.get());
        level.sendBlockUpdated(block.get(), state, state, Block.UPDATE_ALL);
        return CommandUtil.ok(source, "spawner.done", id);
    }

    private static int spawnMob(CommandSourceStack source, ResourceLocation id, int amount, ServerPlayer at)
            throws CommandSyntaxException {
        Optional<EntityType<?>> type = ItemLookup.entity(id);
        if (type.isEmpty() || !type.get().canSummon()) {
            return CommandUtil.fail(source, "mob.unknown", id);
        }
        int limit = ConfigManager.get().spawnmobLimit;
        int count = Math.min(amount, limit);
        ServerLevel level;
        Vec3 position;
        if (at != null) {
            level = Players.level(at);
            position = at.position();
        } else {
            ServerPlayer player = source.getPlayerOrException();
            level = Players.level(player);
            Optional<BlockPos> block = lookedAt(player);
            position = block.map(pos -> Vec3.atBottomCenterOf(pos.above())).orElse(player.position());
        }
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Entity entity = type.get().create(level);
            if (entity != null) {
                entity.moveTo(position.x, position.y, position.z, level.getRandom().nextFloat() * 360, 0);
                level.addFreshEntity(entity);
                spawned++;
            }
        }
        return CommandUtil.ok(source, "spawnmob.done", spawned, id);
    }

    /** Plants fully grown saplings and lets vanilla grow them, so every version gets its own tree. */
    private static int tree(CommandSourceStack source, String rawType, boolean big) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = Players.level(player);
        String type = rawType.toLowerCase(Locale.ROOT);
        Block sapling = (big ? BIG_TREES : TREES).get(type);
        if (sapling == null) {
            return CommandUtil.fail(source, "tree.invalid", String.join(", ", (big ? BIG_TREES : TREES).keySet()));
        }
        Optional<BlockPos> block = lookedAt(player);
        if (block.isEmpty()) {
            return CommandUtil.fail(source, "jump.nothing");
        }
        BlockPos pos = block.get().above();
        List<BlockPos> spots = big ? List.of(pos, pos.east(), pos.south(), pos.south().east()) : List.of(pos);
        for (BlockPos spot : spots) {
            if (!level.getBlockState(spot).isAir()) {
                return CommandUtil.fail(source, "tree.blocked");
            }
        }
        BlockState state = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
        spots.forEach(spot -> level.setBlock(spot, state, Block.UPDATE_ALL));
        ((SaplingBlock) sapling).advanceTree(level, pos, state, level.getRandom());
        if (level.getBlockState(pos).is(sapling)) {
            spots.forEach(spot -> level.removeBlock(spot, false));
            return CommandUtil.fail(source, "tree.blocked");
        }
        return CommandUtil.ok(source, "tree.done");
    }

    private static int breakBlock(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = Players.level(player);
        Optional<BlockPos> block = lookedAt(player);
        if (block.isEmpty()) {
            return CommandUtil.fail(source, "jump.nothing");
        }
        BlockState state = level.getBlockState(block.get());
        if (state.getDestroySpeed(level, block.get()) < 0 && !PermissionNodes.BREAK_BEDROCK.check(source)) {
            return CommandUtil.fail(source, "break.unbreakable");
        }
        level.destroyBlock(block.get(), false);
        return 1;
    }
}
