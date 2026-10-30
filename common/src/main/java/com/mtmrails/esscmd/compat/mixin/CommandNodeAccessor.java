package com.mtmrails.esscmd.compat.mixin;

import java.util.Map;
import com.mojang.brigadier.tree.CommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets overrideVanilla remove a root command before the replacement is registered. */
@Mixin(value = CommandNode.class, remap = false)
public interface CommandNodeAccessor<S> {
    @Accessor("children")
    Map<String, CommandNode<S>> cmdessentials$children();

    @Accessor("literals")
    Map<String, CommandNode<S>> cmdessentials$literals();

    @Accessor("arguments")
    Map<String, CommandNode<S>> cmdessentials$arguments();
}
