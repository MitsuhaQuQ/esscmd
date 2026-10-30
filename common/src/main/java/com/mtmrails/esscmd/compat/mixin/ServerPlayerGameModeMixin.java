package com.mtmrails.esscmd.compat.mixin;

import com.mtmrails.esscmd.player.ItemPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Right-clicking with a powertool runs its command instead of using the item. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void cmdessentials$useItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                       CallbackInfoReturnable<InteractionResult> cir) {
        if (ItemPowers.runPowerTool(player, stack)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void cmdessentials$useItemOn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                         BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (ItemPowers.runPowerTool(player, stack)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
