package org.gloyer057.cuffsx;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import org.gloyer057.cuffsx.command.CuffsxCommand;
import org.gloyer057.cuffsx.cuff.*;
import org.gloyer057.cuffsx.item.ModItems;
import org.gloyer057.cuffsx.network.NetworkIds;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Cuffsx implements ModInitializer {

    private static final Map<UUID, Long> callbackCooldown = new ConcurrentHashMap<>();
    private static final long CALLBACK_COOLDOWN_MS = 500L;

    @Override
    public void onInitialize() {
        ModItems.register();

        // Блокировка взаимодействий для HANDS
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (player instanceof ServerPlayerEntity sp && CuffManager.isCuffed(sp, CuffType.HANDS))
                return ActionResult.SUCCESS;
            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (player instanceof ServerPlayerEntity sp && CuffManager.isCuffed(sp, CuffType.HANDS))
                return ActionResult.SUCCESS;
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient()) return TypedActionResult.pass(player.getStackInHand(hand));
            if (player instanceof ServerPlayerEntity sp && CuffManager.isCuffed(sp, CuffType.HANDS))
                return TypedActionResult.fail(player.getStackInHand(hand));
            return TypedActionResult.pass(player.getStackInHand(hand));
        });

        // Ломание наручников от клиента
        ServerPlayNetworking.registerGlobalReceiver(NetworkIds.CUFF_BREAK, (server, player, handler, buf, responseSender) -> {
            String type = buf.readString();
            server.execute(() -> {
                UUID uuid = player.getUuid();
                if ("HANDS".equals(type) && CuffManager.isCuffed(player, CuffType.HANDS)) {
                    if (CuffDurability.damageHands(uuid)) {
                        CuffManager.removeCuffsForced(player, CuffType.HANDS);
                        player.sendMessage(new LiteralText("§aВы сломали наручники на руках!"), true);
                    } else CuffManager.sendHud(player);
                } else if ("LEGS".equals(type) && CuffManager.isCuffed(player, CuffType.LEGS)) {
                    if (CuffDurability.damageLegs(uuid)) {
                        CuffManager.removeCuffsForced(player, CuffType.LEGS);
                        player.sendMessage(new LiteralText("§aВы сломали наручники на ногах!"), true);
                    } else CuffManager.sendHud(player);
                }
            });
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            CuffLog.setServer(server);
            CuffManager.onTick();
            long now = System.currentTimeMillis();
            callbackCooldown.entrySet().removeIf(e -> now - e.getValue() > 2000L);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickLegs(server);
            tickLeash(server);
            tickApplyProgress(server);
            tickRemoveProgress(server);
            tickHudSync(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            CuffState state = CuffState.getOrCreate(server);
            state.getRecords(player.getUuid()).forEach(r -> {
                CuffManager.applyRestrictions(player, r.cuffType());
                if (r.cuffType() == CuffType.HANDS && CuffDurability.getHandsHp(player.getUuid()) < 0)
                    CuffDurability.initHands(player.getUuid());
                else if (r.cuffType() == CuffType.LEGS && CuffDurability.getLegsHp(player.getUuid()) < 0)
                    CuffDurability.initLegs(player.getUuid());
            });
            CuffManager.syncToClients(player);
            CuffManager.sendHud(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            // Если игрок был на поводке — обновляем lockedPos на текущую позицию,
            // чтобы при перезаходе он оставался там же, а не телепортировался к месту надевания
            if (LeashManager.isLeashed(player.getUuid())) {
                CuffState state = CuffState.getOrCreate(server);
                for (CuffType t : CuffType.values()) {
                    if (state.hasCuff(player.getUuid(), t)) {
                        state.updateLockedPos(player.getUuid(), t, player.getPos());
                    }
                }
                // Сообщение ведущему
                UUID holderUuid = LeashManager.getHolder(player.getUuid());
                if (holderUuid != null) {
                    ServerPlayerEntity holder = server.getPlayerManager().getPlayer(holderUuid);
                    if (holder != null) {
                        holder.sendMessage(new LiteralText("§eИгрок которого вы вели ливнул."), true);
                    }
                }
                LeashManager.detachByLeashed(player.getUuid());
            }
            // Если игрок был ведущим — сообщение ведомому
            UUID leashedUuid = LeashManager.getLeashed(player.getUuid());
            if (leashedUuid != null) {
                ServerPlayerEntity leashed = server.getPlayerManager().getPlayer(leashedUuid);
                if (leashed != null) {
                    leashed.sendMessage(new LiteralText("§eИгрок ведущий вас вышел из игры."), true);
                }
                LeashManager.detachByHolder(player.getUuid());
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
            CuffsxCommand.register(dispatcher));
    }

    private void tickLegs(MinecraftServer server) {
        CuffState state = CuffState.getOrCreate(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (LeashManager.isLeashed(player.getUuid())) continue;
            state.getRecords(player.getUuid()).stream()
                .filter(r -> r.cuffType() == CuffType.LEGS).findFirst()
                .ifPresent(r -> {
                    if (player.getPos().distanceTo(r.lockedPos()) > 0.1)
                        player.teleport(r.lockedPos().x, r.lockedPos().y, r.lockedPos().z);
                });
        }
    }

    private static int leashTick = 0;
    private void tickLeash(MinecraftServer server) {
        if (++leashTick < 10) return;
        leashTick = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID holderUuid = LeashManager.getHolder(player.getUuid());
            if (holderUuid == null) continue;
            ServerPlayerEntity holder = server.getPlayerManager().getPlayer(holderUuid);
            if (holder == null) { LeashManager.detachByLeashed(player.getUuid()); continue; }
            if (!CuffManager.isCuffed(player)) { LeashManager.detachByLeashed(player.getUuid()); continue; }
            if (player.getPos().distanceTo(holder.getPos()) > 3.0) {
                Vec3d t = holder.getPos();
                player.teleport(t.x, t.y, t.z);
            }
        }
    }

    private void tickApplyProgress(MinecraftServer server) {
        for (ServerPlayerEntity applier : server.getPlayerManager().getPlayerList()) {
            if (!ApplyProgress.isActive(applier.getUuid())) continue;
            if (RemoveProgress.isActive(applier.getUuid())) continue;
            ApplyProgress.Entry entry = ApplyProgress.get(applier.getUuid());
            if (entry == null) continue;
            ServerPlayerEntity target = server.getPlayerManager().getPlayer(entry.targetUuid());
            if (target == null) { ApplyProgress.cancel(applier.getUuid()); CuffManager.sendHud(applier, -1); continue; }
            ApplyProgress.TickResult result = ApplyProgress.tick(applier.getUuid(), target.getPos());
            switch (result) {
                case IN_PROGRESS -> {
                    int pct = ApplyProgress.getProgress(applier.getUuid());
                    applier.sendMessage(new LiteralText("§eНадевание: " + pct + "%"), true);
                    CuffManager.sendHud(applier, pct);
                }
                case COMPLETED -> {
                    CuffManager.applyCuffsImmediate(applier, target, entry.cuffType());
                    applier.sendMessage(new LiteralText("§aНаручники надеты!"), true);
                    CuffManager.sendHud(applier, -1);
                }
                case CANCELLED_MOVED -> {
                    applier.sendMessage(new LiteralText("§cЦель сдвинулась — надевание отменено."), true);
                    CuffManager.sendHud(applier, -1);
                }
            }
        }
    }

    private void tickRemoveProgress(MinecraftServer server) {
        for (ServerPlayerEntity applier : server.getPlayerManager().getPlayerList()) {
            if (!RemoveProgress.isActive(applier.getUuid())) continue;
            RemoveProgress.Entry entry = RemoveProgress.get(applier.getUuid());
            if (entry == null) continue;
            ServerPlayerEntity target = server.getPlayerManager().getPlayer(entry.targetUuid());
            if (target == null || !CuffManager.isCuffed(target, entry.cuffType())) {
                RemoveProgress.cancel(applier.getUuid()); CuffManager.sendHud(applier, -1); continue;
            }
            RemoveProgress.TickResult result = RemoveProgress.tick(applier.getUuid());
            switch (result) {
                case IN_PROGRESS -> {
                    int pct = RemoveProgress.getProgress(applier.getUuid());
                    applier.sendMessage(new LiteralText("§eСнятие: " + pct + "%"), true);
                    CuffManager.sendHud(applier, pct);
                }
                case COMPLETED -> {
                    CuffManager.removeCuffsByType(applier, target, entry.cuffType());
                    applier.sendMessage(new LiteralText("§aНаручники сняты!"), true);
                    CuffManager.sendHud(applier, -1);
                }
            }
        }
    }

    private static int hudSyncTick = 0;
    private void tickHudSync(MinecraftServer server) {
        if (++hudSyncTick < 20) return;
        hudSyncTick = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (CuffManager.isCuffed(player)) {
                CuffManager.sendHud(player);
                sendCuffStatusActionBar(player);
            }
        }
    }

    private void sendCuffStatusActionBar(ServerPlayerEntity player) {
        boolean hands = CuffManager.isCuffed(player, CuffType.HANDS);
        boolean legs  = CuffManager.isCuffed(player, CuffType.LEGS);
        if (!hands && !legs) return;
        String type;
        if (hands && legs) type = "руки и ноги";
        else if (hands)    type = "руки";
        else               type = "ноги";
        // Отправляем только если нет активного прогресса надевания/снятия
        // (чтобы не перебивать прогресс-бар)
        if (ApplyProgress.isActive(player.getUuid()) || RemoveProgress.isActive(player.getUuid())) return;
        player.sendMessage(new LiteralText("§cВы в наручниках [" + type + "]"), true);
    }
}
