package org.gloyer057.cuffsx.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.gloyer057.cuffsx.cuff.*;
import org.gloyer057.cuffsx.item.HandcuffsItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void cuffsx_onInteract(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (hand != Hand.MAIN_HAND) return;
        if (!((Object) this instanceof ServerPlayerEntity self)) return;
        if (!(entity instanceof ServerPlayerEntity target)) return;
        if (self == target) return;

        // Поводок
        if (self.getMainHandStack().isOf(Items.LEAD)) {
            if (!CuffManager.isCuffed(self) && CuffManager.isCuffed(target)) {
                if (LeashManager.isLeashed(target.getUuid())) {
                    UUID holderUuid = LeashManager.getHolder(target.getUuid());
                    if (holderUuid != null && holderUuid.equals(self.getUuid())) {
                        LeashManager.detachByLeashed(target.getUuid());
                        CuffState leashState = CuffState.getOrCreate(self.getServer());
                        for (CuffType t : CuffType.values()) {
                            if (leashState.hasCuff(target.getUuid(), t))
                                leashState.updateLockedPos(target.getUuid(), t, target.getPos());
                        }
                        self.sendMessage(new LiteralText("§7Поводок снят."), true);
                    } else {
                        self.sendMessage(new LiteralText("§c" + target.getName().getString() + " уже на поводке."), true);
                    }
                } else {
                    LeashManager.attach(self, target);
                    self.sendMessage(new LiteralText("§7Поводок надет."), true);
                }
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }
        }

        // Надевание
        if (self.getMainHandStack().getItem() instanceof HandcuffsItem hi) {
            if (CuffManager.justRemoved(self.getUuid())) { cir.setReturnValue(ActionResult.SUCCESS); return; }
            if (!CuffManager.isEnabled()) {
                self.sendMessage(new LiteralText("§cНаручники нельзя одеть в данный момент."), false);
                cir.setReturnValue(ActionResult.SUCCESS); return;
            }
            if (CuffManager.isCuffed(target)) {
                self.getMainHandStack().decrement(1);
                CuffManager.applyCuffsImmediate(self, target, hi.getCuffType());
                cir.setReturnValue(ActionResult.SUCCESS); return;
            }
            if (ApplyProgress.isActive(self.getUuid())) {
                ApplyProgress.Entry entry = ApplyProgress.get(self.getUuid());
                if (entry != null && entry.targetUuid().equals(target.getUuid())) {
                    net.minecraft.item.ItemStack returnStack = new net.minecraft.item.ItemStack(
                        entry.cuffType() == CuffType.HANDS
                            ? org.gloyer057.cuffsx.item.ModItems.HANDCUFFS_HANDS
                            : org.gloyer057.cuffsx.item.ModItems.HANDCUFFS_LEGS);
                    if (!self.getInventory().insertStack(returnStack)) self.dropItem(returnStack, false);
                    ApplyProgress.cancel(self.getUuid());
                    CuffManager.sendHud(self, -1);
                    self.sendMessage(new LiteralText("§cНадевание отменено."), true);
                    cir.setReturnValue(ActionResult.SUCCESS); return;
                }
            }
            self.getMainHandStack().decrement(1);
            ApplyProgress.start(self, target, hi.getCuffType());
            cir.setReturnValue(ActionResult.SUCCESS); return;
        }

        // Снятие
        if (!(self.getMainHandStack().getItem() instanceof HandcuffsItem)
                && !(self.getOffHandStack().getItem() instanceof HandcuffsItem)
                && !CuffManager.isCuffed(self) && CuffManager.isCuffed(target)) {

            CuffType typeToRemove;
            if (self.getPos().y + self.getHeight() / 2.0 < target.getPos().y + target.getHeight() / 2.0) {
                typeToRemove = CuffType.LEGS;
            } else {
                typeToRemove = CuffType.HANDS;
            }
            CuffState state = CuffState.getOrCreate(self.getServer());
            if (!state.hasCuff(target.getUuid(), typeToRemove))
                typeToRemove = typeToRemove == CuffType.HANDS ? CuffType.LEGS : CuffType.HANDS;
            if (!state.hasCuff(target.getUuid(), typeToRemove)) return;

            if (RemoveProgress.isActive(self.getUuid())) {
                RemoveProgress.Entry entry = RemoveProgress.get(self.getUuid());
                if (entry != null && entry.targetUuid().equals(target.getUuid())) {
                    RemoveProgress.cancel(self.getUuid());
                    CuffManager.sendHud(self, -1);
                    self.sendMessage(new LiteralText("§cСнятие отменено."), true);
                    cir.setReturnValue(ActionResult.SUCCESS); return;
                }
            }
            final CuffType finalType = typeToRemove;
            RemoveProgress.start(self, target, finalType);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void cuffsx_onAttack(Entity target, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity self)) return;
        if (CuffManager.isCuffed(self, CuffType.HANDS)) ci.cancel();
    }
}
