package org.gloyer057.cuffsx.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.gloyer057.cuffsx.cuff.CuffManager;
import org.gloyer057.cuffsx.cuff.CuffType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void cuffsx_onJump(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity sp && CuffManager.isCuffed(sp, CuffType.LEGS)) {
            ci.cancel();
        }
    }
}
