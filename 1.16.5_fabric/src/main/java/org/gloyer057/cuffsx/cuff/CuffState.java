package org.gloyer057.cuffsx.cuff;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;

import java.util.*;

public class CuffState extends PersistentState {
    public static final String KEY = "cuffsx_state";

    private final Map<UUID, Set<CuffRecord>> records = new HashMap<>();

    public CuffState() {
        super(KEY);
    }

    public void addRecord(CuffRecord record) {
        records.computeIfAbsent(record.targetUUID(), k -> new HashSet<>()).add(record);
        markDirty();
    }

    public boolean removeRecord(UUID targetUUID, CuffType type) {
        Set<CuffRecord> set = records.get(targetUUID);
        if (set == null) return false;
        boolean removed = set.removeIf(r -> r.cuffType() == type);
        if (set.isEmpty()) records.remove(targetUUID);
        if (removed) markDirty();
        return removed;
    }

    public Set<CuffRecord> getRecords(UUID targetUUID) {
        return records.getOrDefault(targetUUID, Collections.emptySet());
    }

    public Collection<CuffRecord> getAllRecords() {
        List<CuffRecord> all = new ArrayList<>();
        for (Set<CuffRecord> set : records.values()) all.addAll(set);
        return all;
    }

    public boolean hasCuff(UUID targetUUID, CuffType type) {
        Set<CuffRecord> set = records.get(targetUUID);
        if (set == null) return false;
        return set.stream().anyMatch(r -> r.cuffType() == type);
    }

    public void updateLockedPos(UUID targetUUID, CuffType type, Vec3d newPos) {
        Set<CuffRecord> set = records.get(targetUUID);
        if (set == null) return;
        CuffRecord old = set.stream().filter(r -> r.cuffType() == type).findFirst().orElse(null);
        if (old == null) return;
        set.remove(old);
        set.add(new CuffRecord(old.targetUUID(), old.applierUUID(), old.cuffType(),
            old.timestamp(), newPos, old.applierName(), old.targetName()));
        markDirty();
    }

    @Override
    public CompoundTag toTag(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (CuffRecord record : getAllRecords()) {
            list.add(record.toNbt());
        }
        nbt.put("records", list);
        return nbt;
    }

    public static CuffState fromTag(CompoundTag nbt) {
        CuffState state = new CuffState();
        ListTag list = nbt.getList("records", 10);
        for (int i = 0; i < list.size(); i++) {
            CuffRecord record = CuffRecord.fromNbt(list.getCompound(i));
            state.records.computeIfAbsent(record.targetUUID(), k -> new HashSet<>()).add(record);
        }
        return state;
    }

    public static CuffState getOrCreate(MinecraftServer server) {
        return server.getOverworld()
            .getPersistentStateManager()
            .getOrCreate(() -> fromTag(new CompoundTag()), CuffState::new, KEY);
    }
}
