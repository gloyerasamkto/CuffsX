package org.gloyer057.cuffsx.cuff;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LeashManager {
    private static final Map<UUID, UUID> holderToLeashed = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> leashedToHolder = new ConcurrentHashMap<>();

    public static void attach(ServerPlayerEntity holder, ServerPlayerEntity leashed) {
        detachByHolder(holder.getUuid());
        detachByLeashed(leashed.getUuid());
        holderToLeashed.put(holder.getUuid(), leashed.getUuid());
        leashedToHolder.put(leashed.getUuid(), holder.getUuid());
    }

    public static void detachByHolder(UUID holderUuid) {
        UUID leashed = holderToLeashed.remove(holderUuid);
        if (leashed != null) leashedToHolder.remove(leashed);
    }

    public static void detachByLeashed(UUID leashedUuid) {
        UUID holder = leashedToHolder.remove(leashedUuid);
        if (holder != null) holderToLeashed.remove(holder);
    }

    public static boolean isLeashed(UUID uuid) { return leashedToHolder.containsKey(uuid); }
    public static UUID getHolder(UUID leashedUuid) { return leashedToHolder.get(leashedUuid); }
    public static UUID getLeashed(UUID holderUuid) { return holderToLeashed.get(holderUuid); }
}
