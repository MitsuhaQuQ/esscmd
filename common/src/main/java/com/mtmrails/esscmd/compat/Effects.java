package com.mtmrails.esscmd.compat;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
//? if >=1.20.5 {
/*import net.minecraft.core.Holder;
*///?}

/** Status-effect helpers; effects are {@code Holder}s from 1.20.5. */
public final class Effects {
    private Effects() {}

    /** Removes every harmful effect (poison, wither, slowness, ...). Returns how many were removed. */
    public static int removeHarmful(ServerPlayer player) {
        //? if >=1.20.5 {
        /*List<Holder<MobEffect>> harmful = player.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .filter(effect -> effect.value().getCategory() == MobEffectCategory.HARMFUL)
                .toList();
        *///?} else {
        List<MobEffect> harmful = player.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .filter(effect -> effect.getCategory() == MobEffectCategory.HARMFUL)
                .toList();
        //?}
        harmful.forEach(player::removeEffect);
        return harmful.size();
    }
}
