package com.mtmrails.esscmd.forge;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import com.mtmrails.esscmd.CmdEssentials;
import com.mtmrails.esscmd.permission.IntOption;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.platform.Platform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

public final class ForgePlatform implements Platform {
    /** Forge nodes for {@link PermissionNodes#ALL}, keyed by full node name. */
    static final Map<String, PermissionNode<Boolean>> NODES = PermissionNodes.ALL.stream()
            .collect(Collectors.toUnmodifiableMap(com.mtmrails.esscmd.permission.PermissionNode::name, ForgePlatform::toForgeNode));

    /** Forge nodes for {@link PermissionNodes#INT_OPTIONS}, keyed by full option name. */
    static final Map<String, PermissionNode<Integer>> INT_NODES = PermissionNodes.INT_OPTIONS.stream()
            .collect(Collectors.toUnmodifiableMap(IntOption::name, ForgePlatform::toIntNode));

    private static PermissionNode<Integer> toIntNode(IntOption option) {
        return new PermissionNode<>(CmdEssentials.MOD_ID, nodeName(option.name()), PermissionTypes.INTEGER,
                (player, uuid, context) -> option.fallback().getAsInt());
    }

    /** Forge builds the full name as "<modid>.<nodeName>". */
    private static String nodeName(String fullName) {
        return fullName.substring(CmdEssentials.MOD_ID.length() + 1);
    }

    private static PermissionNode<Boolean> toForgeNode(com.mtmrails.esscmd.permission.PermissionNode node) {
        return new PermissionNode<>(CmdEssentials.MOD_ID, nodeName(node.name()), PermissionTypes.BOOLEAN,
                (player, uuid, context) -> player != null && player.hasPermissions(node.opLevel()));
    }

    @Override
    public String loaderName() {
        return "Forge";
    }

    @Override
    public String modVersion() {
        return ModList.get().getModContainerById(CmdEssentials.MOD_ID)
                .map(mod -> mod.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean hasPermission(CommandSourceStack source, String node, int opLevel) {
        PermissionNode<Boolean> forgeNode = NODES.get(node);
        if (forgeNode != null && source.getEntity() instanceof ServerPlayer player) {
            return PermissionAPI.getPermission(player, forgeNode);
        }
        // Console, command blocks and unknown nodes use the OP level.
        return source.hasPermission(opLevel);
    }

    @Override
    public int getInt(ServerPlayer player, IntOption option) {
        PermissionNode<Integer> node = INT_NODES.get(option.name());
        return node != null ? PermissionAPI.getPermission(player, node) : option.fallback().getAsInt();
    }
}
