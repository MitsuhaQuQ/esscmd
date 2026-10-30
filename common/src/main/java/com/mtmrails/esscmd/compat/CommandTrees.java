package com.mtmrails.esscmd.compat;

import java.lang.reflect.Field;
import java.util.Map;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;

/**
 * Removes commands from Brigadier's tree, which has no API for it. Uses reflection, not a
 * mixin: Forge doesn't transform Brigadier, so an accessor mixin crashes there at startup.
 */
public final class CommandTrees {
    private CommandTrees() {}

    public static <S> void removeRootCommand(CommandDispatcher<S> dispatcher, String name) {
        CommandNode<S> root = dispatcher.getRoot();
        for (String field : new String[] {"children", "literals", "arguments"}) {
            try {
                Field map = CommandNode.class.getDeclaredField(field);
                map.setAccessible(true);
                ((Map<?, ?>) map.get(root)).remove(name);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot remove /" + name + " from the command tree", e);
            }
        }
    }
}
