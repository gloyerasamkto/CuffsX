package org.gloyer057.cuffsx.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.gloyer057.cuffsx.cuff.*;
import org.gloyer057.cuffsx.item.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathMixin {

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void cuffsx_onDeath(DamageSource source, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        CuffState state = CuffState.getOrCreate(player.getServer());
        for (CuffType type : CuffType.values()) {
            if (state.hasCuff(player.getUuid(), type)) {
                ItemStack drop = new ItemStack(type == CuffType.HANDS ? ModItems.HANDCUFFS_HANDS : ModItems.HANDCUFFS_LEGS);
                player.dropItem(drop, true, false);
                CuffManager.removeCuffsForced(player, type);
            }
        }
        LeashManager.detachByLeashed(player.getUuid());
    }
}
