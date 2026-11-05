package com.mtmrails.esscmd.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.19.3 {
import net.minecraft.core.registries.BuiltInRegistries;
//?} else {
/*import net.minecraft.core.Registry;
*///?}
//? if >=1.20.5 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
*///?} else {
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.alchemy.PotionUtils;
//?}

/**
 * Custom effects on potion items: the {@code CustomPotionEffects} tag before 1.20.5, the
 * {@code potion_contents} component after. Effects are {@code Holder}s from 1.20.5.
 */
public final class Potions {
    private Potions() {}

    /** Drinkable, splash and lingering potions. */
    public static boolean isPotion(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    /** A new effect instance, or empty if {@code id} is not a status effect. */
    public static Optional<MobEffectInstance> effect(ResourceLocation id, int durationTicks, int amplifier) {
        //? if >=1.20.5 {
        /*return BuiltInRegistries.MOB_EFFECT.getOptional(id)
                .map(effect -> new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), durationTicks, amplifier));
        *///?} else if >=1.19.3 {
        return BuiltInRegistries.MOB_EFFECT.getOptional(id).map(effect -> new MobEffectInstance(effect, durationTicks, amplifier));
        //?} else {
        /*return Registry.MOB_EFFECT.getOptional(id).map(effect -> new MobEffectInstance(effect, durationTicks, amplifier));
        *///?}
    }

    public static Iterable<ResourceLocation> effectIds() {
        //? if >=1.19.3 {
        return BuiltInRegistries.MOB_EFFECT.keySet();
        //?} else {
        /*return Registry.MOB_EFFECT.keySet();
        *///?}
    }

    /** The effect's translated name, which every client can render. */
    public static Component name(MobEffectInstance instance) {
        return type(instance).getDisplayName();
    }

    /** The namespaced effect id, such as {@code minecraft:speed}. */
    public static ResourceLocation id(MobEffectInstance instance) {
        //? if >=1.19.3 {
        return BuiltInRegistries.MOB_EFFECT.getKey(type(instance));
        //?} else {
        /*return Registry.MOB_EFFECT.getKey(type(instance));
        *///?}
    }

    /** The effects added on top of the potion type. */
    public static List<MobEffectInstance> customEffects(ItemStack stack) {
        //? if >=1.20.5 {
        /*return new ArrayList<>(stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).customEffects());
        *///?} else {
        return new ArrayList<>(PotionUtils.getCustomEffects(stack));
        //?}
    }

    /** The potion type's effects plus the custom ones: what drinking it gives. */
    public static List<MobEffectInstance> allEffects(ItemStack stack) {
        //? if >=1.20.5 {
        /*List<MobEffectInstance> effects = new ArrayList<>();
        stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects().forEach(effects::add);
        return effects;
        *///?} else {
        return new ArrayList<>(PotionUtils.getMobEffects(stack));
        //?}
    }

    /** Replaces the custom effects; the potion type and colour stay. */
    public static void setCustomEffects(ItemStack stack, List<MobEffectInstance> effects) {
        //? if >=26.1 {
        /*PotionContents c = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(c.potion(), c.customColor(), List.copyOf(effects), c.customName()));
        *///?} else if >=1.20.5 {
        /*PotionContents c = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(c.potion(), c.customColor(), List.copyOf(effects)));
        *///?} else {
        CompoundTag tag = stack.getOrCreateTag();
        tag.remove(PotionUtils.TAG_CUSTOM_POTION_EFFECTS);
        if (!effects.isEmpty()) {
            ListTag list = new ListTag();
            effects.forEach(effect -> list.add(effect.save(new CompoundTag())));
            tag.put(PotionUtils.TAG_CUSTOM_POTION_EFFECTS, list);
        }
        //?}
    }

    private static MobEffect type(MobEffectInstance instance) {
        //? if >=1.20.5 {
        /*return instance.getEffect().value();
        *///?} else {
        return instance.getEffect();
        //?}
    }
}
