package org.gloyer057.cuffsx.cuff;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RemoveProgress {
    public static final int REQUIRED_TICKS = 40;

    public static class Entry {
        public final UUID targetUuid;
        public final CuffType cuffType;
        public final int ticksHeld;

        public Entry(UUID targetUuid, CuffType cuffType, int ticksHeld) {
            this.targetUuid = targetUuid;
            this.cuffType = cuffType;
            this.ticksHeld = ticksHeld;
        }

        public UUID targetUuid() { return targetUuid; }
        public CuffType cuffType() { return cuffType; }
        public int ticksHeld() { return ticksHeld; }
    }

    private static final Map<UUID, Entry> progress = new ConcurrentHashMap<>();

    public static void start(ServerPlayerEntity applier, ServerPlayerEntity target, CuffType type) {
        progress.put(applier.getUuid(), new Entry(target.getUuid(), type, 0));
    }

    public static void cancel(UUID applierUuid) { progress.remove(applierUuid); }
    public static Entry get(UUID applierUuid) { return progress.get(applierUuid); }
    public static boolean isActive(UUID applierUuid) { return progress.containsKey(applierUuid); }

    public static TickResult tick(UUID applierUuid) {
        Entry e = progress.get(applierUuid);
        if (e == null) return TickResult.NOT_ACTIVE;
        int newTicks = e.ticksHeld + 1;
        if (newTicks >= REQUIRED_TICKS) {
            progress.remove(applierUuid);
            return TickResult.COMPLETED;
        }
        progress.put(applierUuid, new Entry(e.targetUuid, e.cuffType, newTicks));
        return TickResult.IN_PROGRESS;
    }

    public static int getProgress(UUID applierUuid) {
        Entry e = progress.get(applierUuid);
        if (e == null) return -1;
        return e.ticksHeld * 100 / REQUIRED_TICKS;
    }

    public enum TickResult { NOT_ACTIVE, IN_PROGRESS, COMPLETED }
}
