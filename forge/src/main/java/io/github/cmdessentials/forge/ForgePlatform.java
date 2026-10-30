package io.github.cmdessentials.forge;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import io.github.cmdessentials.CmdEssentials;
import io.github.cmdessentials.permission.PermissionNodes;
import io.github.cmdessentials.platform.Platform;
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
            .collect(Collectors.toUnmodifiableMap(io.github.cmdessentials.permission.PermissionNode::name, ForgePlatform::toForgeNode));

    private static PermissionNode<Boolean> toForgeNode(io.github.cmdessentials.permission.PermissionNode node) {
        // Forge builds the full name as "<modid>.<nodeName>".
        String nodeName = node.name().substring(CmdEssentials.MOD_ID.length() + 1);
        return new PermissionNode<>(CmdEssentials.MOD_ID, nodeName, PermissionTypes.BOOLEAN,
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
}
