package com.mtmrails.esscmd.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

/** Recipe lookup; recipes are wrapped in holders from 1.20.2 and need registries from 1.19.4. */
public final class Recipes {
    private Recipes() {}

    /** The ingredients of a recipe producing {@code item}, each as its first matching stack. */
    public static Optional<List<ItemStack>> find(MinecraftServer server, Item item) {
        for (var entry : server.getRecipeManager().getRecipes()) {
            //? if >=1.20.2 {
            /*Recipe<?> recipe = entry.value();
            *///?} else {
            Recipe<?> recipe = entry;
            //?}
            //? if >=1.19.4 {
            ItemStack result = recipe.getResultItem(server.registryAccess());
            //?} else {
            /*ItemStack result = recipe.getResultItem();
            *///?}
            if (result.getItem() == item) {
                List<ItemStack> ingredients = new ArrayList<>();
                for (Ingredient ingredient : recipe.getIngredients()) {
                    ItemStack[] options = ingredient.getItems();
                    if (options.length > 0) {
                        ingredients.add(options[0]);
                    }
                }
                if (!ingredients.isEmpty()) {
                    return Optional.of(ingredients);
                }
            }
        }
        return Optional.empty();
    }
}
