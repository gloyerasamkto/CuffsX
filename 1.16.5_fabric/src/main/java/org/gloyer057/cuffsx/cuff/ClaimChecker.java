package org.gloyer057.cuffsx.cuff;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

public class ClaimChecker {

    public static boolean isClaimed(ServerPlayerEntity applier, BlockPos pos) {
        if (!FabricLoader.getInstance().isModLoaded("ftbchunks")) return false;
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
            Object api = apiClass.getMethod("api").invoke(null);
            Object manager = api.getClass().getMethod("getManager").invoke(api);

            Class<?> chunkDimPosClass = Class.forName("dev.ftb.mods.ftblibrary.math.ChunkDimPos");
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;

            Object chunkDimPos = null;
            for (Constructor<?> c : chunkDimPosClass.getConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 3) {
                    try { chunkDimPos = c.newInstance(applier.getWorld().getRegistryKey(), chunkX, chunkZ); break; }
                    catch (Exception ignored) {}
                }
            }
            if (chunkDimPos == null) return false;

            Method getChunk = null;
            for (Method m : manager.getClass().getMethods()) {
                if (m.getName().equals("getChunk") && m.getParameterCount() == 1) { getChunk = m; break; }
            }
            if (getChunk == null) return false;

            Object chunk = getChunk.invoke(manager, chunkDimPos);
            if (chunk == null) return false;

            Method getTeamData = null;
            for (Method m : chunk.getClass().getMethods()) {
                if (m.getName().equals("getTeamData") && m.getParameterCount() == 0) { getTeamData = m; break; }
            }
            if (getTeamData == null) return true;

            Object teamData = getTeamData.invoke(chunk);
            if (teamData == null) return true;

            UUID ownerUuid = null;
            for (Method m : teamData.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String name = m.getName();
                if (name.equals("getOwner") || name.equals("getOwnerUUID") || name.equals("getOwnerUuid")) {
                    Object r = m.invoke(teamData);
                    if (r instanceof UUID) { ownerUuid = (UUID) r; break; }
                }
            }
            if (ownerUuid != null && ownerUuid.equals(applier.getUuid())) return false;

            // Проверяем членство в команде
            for (Method m : manager.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                String name = m.getName();
                if (!name.toLowerCase().contains("team") && !name.toLowerCase().contains("player")) continue;
                try {
                    Object applierTeamData = m.invoke(manager, applier.getUuid());
                    if (applierTeamData == null) continue;
                    for (Method tm : applierTeamData.getClass().getMethods()) {
                        if (tm.getName().equals("getTeam") && tm.getParameterCount() == 0) {
                            Object applierTeam = tm.invoke(applierTeamData);
                            for (Method otm : teamData.getClass().getMethods()) {
                                if (otm.getName().equals("getTeam") && otm.getParameterCount() == 0) {
                                    Object ownerTeam = otm.invoke(teamData);
                                    if (applierTeam != null && applierTeam.equals(ownerTeam)) return false;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
