package org.gloyer057.cuffsx.client;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CuffClientState {
    private static final Map<UUID, Set<String>> cuffed = new ConcurrentHashMap<>();

    public static void update(UUID uuid, boolean handsOn, boolean legsOn) {
        Set<String> types = ConcurrentHashMap.newKeySet();
        if (handsOn) types.add("HANDS");
        if (legsOn)  types.add("LEGS");
        if (types.isEmpty()) cuffed.remove(uuid);
        else cuffed.put(uuid, types);
    }

    public static boolean hasHands(UUID uuid) {
        Set<String> s = cuffed.get(uuid);
        return s != null && s.contains("HANDS");
    }

    public static boolean hasLegs(UUID uuid) {
        Set<String> s = cuffed.get(uuid);
        return s != null && s.contains("LEGS");
    }
}
