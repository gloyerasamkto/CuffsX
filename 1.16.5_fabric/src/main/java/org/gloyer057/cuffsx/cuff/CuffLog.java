package org.gloyer057.cuffsx.cuff;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;

public class CuffLog {
    public enum Action { APPLY, REMOVE }

    public static class LogEntry {
        public final long timestamp;
        public final Action action;
        public final String targetName;
        public final String applierName;
        public final CuffType cuffType;
        public final Vec3d coords;

        public LogEntry(long timestamp, Action action, String targetName,
                        String applierName, CuffType cuffType, Vec3d coords) {
            this.timestamp = timestamp;
            this.action = action;
            this.targetName = targetName;
            this.applierName = applierName;
            this.cuffType = cuffType;
            this.coords = coords;
        }

        public long timestamp() { return timestamp; }
        public Action action() { return action; }
        public String targetName() { return targetName; }
        public String applierName() { return applierName; }
        public CuffType cuffType() { return cuffType; }
        public Vec3d coords() { return coords; }
    }

    // TTL — 6 часов
    private static final long TTL_MS = 6 * 60 * 60 * 1000L;

    // ---- PersistentState для сохранения логов между рестартами ----

    public static class LogState extends PersistentState {
        public static final String KEY = "cuffsx_log";
        private final List<LogEntry> entries = new ArrayList<>();

        public LogState() { super(KEY); }

        public void addEntry(LogEntry e) {
            entries.add(e);
            markDirty();
        }

        public List<LogEntry> getEntries() {
            long cutoff = System.currentTimeMillis() - TTL_MS;
            entries.removeIf(e -> e.timestamp < cutoff);
            return new ArrayList<>(entries);
        }

        @Override
        public CompoundTag toTag(CompoundTag nbt) {
            long cutoff = System.currentTimeMillis() - TTL_MS;
            entries.removeIf(e -> e.timestamp < cutoff);
            ListTag list = new ListTag();
            for (LogEntry e : entries) {
                CompoundTag tag = new CompoundTag();
                tag.putLong("timestamp", e.timestamp);
                tag.putString("action", e.action.name());
                tag.putString("targetName", e.targetName);
                tag.putString("applierName", e.applierName);
                tag.putString("cuffType", e.cuffType.toNbt());
                tag.putDouble("x", e.coords.x);
                tag.putDouble("y", e.coords.y);
                tag.putDouble("z", e.coords.z);
                list.add(tag);
            }
            nbt.put("entries", list);
            return nbt;
        }

        public static LogState fromTag(CompoundTag nbt) {
            LogState state = new LogState();
            ListTag list = nbt.getList("entries", 10);
            long cutoff = System.currentTimeMillis() - TTL_MS;
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                long ts = tag.getLong("timestamp");
                if (ts < cutoff) continue; // пропускаем устаревшие
                Action action = Action.valueOf(tag.getString("action"));
                String targetName = tag.getString("targetName");
                String applierName = tag.getString("applierName");
                CuffType cuffType = CuffType.fromNbt(tag.getString("cuffType"));
                Vec3d coords = new Vec3d(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
                state.entries.add(new LogEntry(ts, action, targetName, applierName, cuffType, coords));
            }
            return state;
        }

        public static LogState getOrCreate(MinecraftServer server) {
            return server.getOverworld()
                .getPersistentStateManager()
                .getOrCreate(() -> fromTag(new CompoundTag()), LogState::new, KEY);
        }
    }

    // Текущий сервер — устанавливается при старте
    private static MinecraftServer currentServer;

    public static void setServer(MinecraftServer server) {
        currentServer = server;
    }

    public static void log(Action action, CuffRecord record) {
        LogEntry entry = new LogEntry(System.currentTimeMillis(), action,
            record.targetName(), record.applierName(), record.cuffType(), record.lockedPos());
        if (currentServer != null) {
            LogState.getOrCreate(currentServer).addEntry(entry);
        }
    }

    public static List<LogEntry> getRecent() {
        if (currentServer == null) return new ArrayList<>();
        return LogState.getOrCreate(currentServer).getEntries();
    }
}
