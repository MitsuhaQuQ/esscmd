package com.mtmrails.esscmd.menu;

import com.mtmrails.esscmd.compat.Players;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;

/**
 * Vanilla block menus without the block. They get a real {@link ContainerLevelAccess} and only
 * {@code stillValid} is overridden: with {@code ContainerLevelAccess.NULL} the crafting result
 * isn't computed and items left in the grid are lost on close.
 */
public final class PortableMenus {
    private PortableMenus() {}

    private static ContainerLevelAccess access(Inventory inventory) {
        Player player = inventory.player;
        return ContainerLevelAccess.create(Players.level((ServerPlayer) player), player.blockPosition());
    }

    public static AbstractContainerMenu workbench(int id, Inventory inventory, ServerPlayer player) {
        return new CraftingMenu(id, inventory, access(inventory)) {
            @Override
            public boolean stillValid(Player viewer) {
                return true;
            }
        };
    }

    public static AbstractContainerMenu anvil(int id, Inventory inventory, ServerPlayer player) {
        return new AnvilMenu(id, inventory, access(inventory)) {
            @Override
            public boolean stillValid(Player viewer) {
                return true;
            }
        };
    }

    public static AbstractContainerMenu cartographyTable(int id, Inventory inventory, ServerPlayer player) {
        return new CartographyTableMenu(id, inventory, access(inventory)) {
            @Override
            public boolean stillValid(Player viewer) {
                return true;
            }
        };
    }

    public static AbstractContainerMenu grindstone(int id, Inventory inventory, ServerPlayer player) {
        return new GrindstoneMenu(id, inventory, access(inventory)) {
            @Override
            public boolean stillValid(Player viewer) {
                return true;
            }
        };
    }

    public static AbstractContainerMenu loom(int id, Inventory inventory, ServerPlayer player) {
        return new LoomMenu(id, inventory, access(inventory)) {
            @Override
            public boolean stillValid(Player viewer) {
                return true;
            }
        };
    }

    public static AbstractContainerMenu smithingTable(int id, Inventory inventory, ServerPlayer player) {
        return new SmithingMenu(id, inventory, access(inventory)) {
            @Override
            public boolean stillValid(Player viewer) {
                return true;
            }
        };
    }

    public static AbstractContainerMenu stonecutter(int id, Inventory inventory, ServerPlayer player) {
        return new StonecutterMenu(id, inventory, access(inventory)) {
            @Override
            public boolean stillValid(Player viewer) {
                return true;
            }
        };
    }
}
