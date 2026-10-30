package com.mtmrails.esscmd.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A live view of a player's 41 inventory slots (36 main, 4 armor, offhand), padded to the 54
 * slots of a chest menu. The padding is locked by {@link InvseeMenu}.
 */
final class InvseeContainer implements Container {
    static final int SIZE = 54;

    private final ServerPlayer target;
    private final Inventory inventory;

    InvseeContainer(ServerPlayer target) {
        this.target = target;
        this.inventory = target.getInventory();
    }

    static boolean isReal(int slot, Inventory inventory) {
        return slot < inventory.getContainerSize();
    }

    private boolean real(int slot) {
        return isReal(slot, inventory);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return real(slot) ? inventory.getItem(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return real(slot) ? inventory.removeItem(slot, count) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return real(slot) ? inventory.removeItemNoUpdate(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (real(slot)) {
            inventory.setItem(slot, stack);
        }
    }

    @Override
    public void setChanged() {
        inventory.setChanged();
    }

    @Override
    public boolean stillValid(Player viewer) {
        return !target.hasDisconnected() && target.isAlive();
    }

    @Override
    public void clearContent() {
        // The viewer can't clear through a menu; never wipe the target from here.
    }
}
