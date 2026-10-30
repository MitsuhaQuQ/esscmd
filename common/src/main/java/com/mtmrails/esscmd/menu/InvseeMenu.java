package com.mtmrails.esscmd.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * /invsee as a six-row chest menu. Padding slots never accept items. Without
 * {@code editable} every click is ignored, so the view is read-only.
 */
public final class InvseeMenu extends ChestMenu {
    private final boolean editable;

    public InvseeMenu(int id, Inventory viewer, ServerPlayer target, boolean editable) {
        super(MenuType.GENERIC_9x6, id, viewer, new InvseeContainer(target), 6);
        this.editable = editable;
        Inventory watched = target.getInventory();
        for (int i = 0; i < InvseeContainer.SIZE; i++) {
            if (!InvseeContainer.isReal(i, watched)) {
                Slot old = slots.get(i);
                Slot locked = new Slot(old.container, old.getContainerSlot(), old.x, old.y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }
                };
                locked.index = i;
                slots.set(i, locked);
            }
        }
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (editable) {
            super.clicked(slot, button, type, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return editable ? super.quickMoveStack(player, slot) : ItemStack.EMPTY;
    }
}
