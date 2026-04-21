package org.gloyer057.cuffsx.cuff;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.gloyer057.cuffsx.item.ModItems;
import org.gloyer057.cuffsx.network.NetworkIds;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CuffManager {

    private static boolean enabled = true;

    private static final Map<UUID, Long> lastActionTick = new ConcurrentHashMap<>();
    private static long currentTick = 0;

    private static final Set<UUID> appliedThisTick = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> removedAtTick = new ConcurrentHashMap<>();
    private static final long REMOVE_COOLDOWN_TICKS = 20L;

    public static void onTick() {
        currentTick++;
        appliedThisTick.clear();
    }

    public static boolean justApplied(UUID uuid) { return appliedThisTick.contains(uuid); }
    public static boolean justRemoved(UUID uuid) {
        Long tick = removedAtTick.get(uuid);
        return tick != null && currentTick - tick < REMOVE_COOLDOWN_TICKS;
    }

    private static final UUID LEGS_SPEED_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String LEGS_SPEED_NAME = "cuffsx_legs_speed";

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean v) { enabled = v; }

    public static boolean applyCuffs(ServerPlayerEntity applier, ServerPlayerEntity target, CuffType type) {
        if (applier == target) return false;
        Long lastTick = lastActionTick.get(applier.getUuid());
        if (lastTick != null && currentTick - lastTick < 2) return false;
        lastActionTick.put(applier.getUuid(), currentTick);
        if (!enabled) {
            applier.sendMessage(net.minecraft.text.LiteralText.EMPTY.shallowCopy()
                .append("Наручники нельзя одеть в данный момент."), false);
            return false;
        }
        CuffState state = CuffState.getOrCreate(applier.getServer());
        if (state.hasCuff(target.getUuid(), type)) return false;
        CuffRecord record = new CuffRecord(target.getUuid(), applier.getUuid(), type,
            System.currentTimeMillis(), target.getPos(), applier.getName().getString(), target.getName().getString());
        state.addRecord(record);
        applier.getMainHandStack().decrement(1);
        CuffLog.log(CuffLog.Action.APPLY, record);
        applyRestrictions(target, type);
        appliedThisTick.add(applier.getUuid());
        if (type == CuffType.HANDS) CuffDurability.initHands(target.getUuid());
        else CuffDurability.initLegs(target.getUuid());
        syncToClients(target);
        sendHud(target);
        return true;
    }

    public static boolean applyCuffsImmediate(ServerPlayerEntity applier, ServerPlayerEntity target, CuffType type) {
        if (!enabled) {
            applier.sendMessage(net.minecraft.text.LiteralText.EMPTY.shallowCopy()
                .append("Наручники нельзя одеть в данный момент."), false);
            return false;
        }
        CuffState state = CuffState.getOrCreate(applier.getServer());
        if (state.hasCuff(target.getUuid(), type)) return false;
        CuffRecord record = new CuffRecord(target.getUuid(), applier.getUuid(), type,
            System.currentTimeMillis(), target.getPos(), applier.getName().getString(), target.getName().getString());
        state.addRecord(record);
        CuffLog.log(CuffLog.Action.APPLY, record);
        applyRestrictions(target, type);
        if (type == CuffType.HANDS) CuffDurability.initHands(target.getUuid());
        else CuffDurability.initLegs(target.getUuid());
        syncToClients(target);
        sendHud(target);
        return true;
    }

    public static boolean removeCuffsByType(ServerPlayerEntity applier, ServerPlayerEntity target, CuffType type) {
        if (applier == target) return false;
        if (appliedThisTick.contains(applier.getUuid())) return false;
        Long lastTick = lastActionTick.get(applier.getUuid());
        if (lastTick != null && currentTick - lastTick < 2) return false;
        lastActionTick.put(applier.getUuid(), currentTick);
        if (isCuffed(applier)) return false;
        CuffState state = CuffState.getOrCreate(applier.getServer());
        if (!state.hasCuff(target.getUuid(), type)) return false;
        CuffRecord removedRecord = state.getRecords(target.getUuid()).stream()
            .filter(r -> r.cuffType() == type).findFirst().orElse(null);
        state.removeRecord(target.getUuid(), type);
        ItemStack stack = new ItemStack(type == CuffType.HANDS ? ModItems.HANDCUFFS_HANDS : ModItems.HANDCUFFS_LEGS);
        if (!applier.getInventory().insertStack(stack)) applier.dropItem(stack, false);
        if (removedRecord != null) CuffLog.log(CuffLog.Action.REMOVE, removedRecord);
        removeRestrictions(target, type);
        if (type == CuffType.HANDS) CuffDurability.removeHands(target.getUuid());
        else CuffDurability.removeLegs(target.getUuid());
        LeashManager.detachByLeashed(target.getUuid());
        removedAtTick.put(applier.getUuid(), currentTick);
        syncToClients(target);
        sendHud(target);
        return true;
    }

    public static void removeCuffsForced(ServerPlayerEntity target, CuffType type) {
        CuffState state = CuffState.getOrCreate(target.getServer());
        CuffRecord removedRecord = state.getRecords(target.getUuid()).stream()
            .filter(r -> r.cuffType() == type).findFirst().orElse(null);
        state.removeRecord(target.getUuid(), type);
        if (removedRecord != null) CuffLog.log(CuffLog.Action.REMOVE, removedRecord);
        removeRestrictions(target, type);
        if (type == CuffType.HANDS) CuffDurability.removeHands(target.getUuid());
        else CuffDurability.removeLegs(target.getUuid());
        LeashManager.detachByLeashed(target.getUuid());
        syncToClients(target);
        sendHud(target);
    }

    public static boolean isCuffed(ServerPlayerEntity player, CuffType type) {
        return CuffState.getOrCreate(player.getServer()).hasCuff(player.getUuid(), type);
    }

    public static boolean isCuffed(ServerPlayerEntity player) {
        return isCuffed(player, CuffType.HANDS) || isCuffed(player, CuffType.LEGS);
    }

    public static void syncToClients(ServerPlayerEntity target) {
        CuffState state = CuffState.getOrCreate(target.getServer());
        boolean hands = state.hasCuff(target.getUuid(), CuffType.HANDS);
        boolean legs  = state.hasCuff(target.getUuid(), CuffType.LEGS);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(target.getUuid());
        buf.writeBoolean(hands);
        buf.writeBoolean(legs);
        for (ServerPlayerEntity p : target.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, NetworkIds.CUFF_SYNC, buf);
        }
    }

    public static void sendHud(ServerPlayerEntity player) { sendHud(player, -1); }

    public static void sendHud(ServerPlayerEntity player, int applyProgress) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(CuffDurability.getHandsHp(player.getUuid()));
        buf.writeInt(CuffDurability.getLegsHp(player.getUuid()));
        buf.writeInt(applyProgress);
        ServerPlayNetworking.send(player, NetworkIds.CUFF_HUD, buf);
    }

    public static void applyRestrictions(ServerPlayerEntity player, CuffType type) {
        if (type == CuffType.LEGS) {
            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (attr != null) {
                attr.removeModifier(LEGS_SPEED_UUID);
                attr.addPersistentModifier(new EntityAttributeModifier(LEGS_SPEED_UUID, LEGS_SPEED_NAME,
                    -attr.getBaseValue(), EntityAttributeModifier.Operation.ADDITION));
            }
        }
    }

    public static void removeRestrictions(ServerPlayerEntity player, CuffType type) {
        if (type == CuffType.LEGS) {
            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (attr != null) attr.removeModifier(LEGS_SPEED_UUID);
        }
    }
}
