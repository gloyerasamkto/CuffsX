package org.gloyer057.cuffsx.cuff;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class CuffRecord {
    private final UUID targetUUID;
    private final UUID applierUUID;
    private final CuffType cuffType;
    private final long timestamp;
    private final Vec3d lockedPos;
    private final String applierName;
    private final String targetName;

    public CuffRecord(UUID targetUUID, UUID applierUUID, CuffType cuffType,
                      long timestamp, Vec3d lockedPos, String applierName, String targetName) {
        this.targetUUID = targetUUID;
        this.applierUUID = applierUUID;
        this.cuffType = cuffType;
        this.timestamp = timestamp;
        this.lockedPos = lockedPos;
        this.applierName = applierName;
        this.targetName = targetName;
    }

    public UUID targetUUID() { return targetUUID; }
    public UUID applierUUID() { return applierUUID; }
    public CuffType cuffType() { return cuffType; }
    public long timestamp() { return timestamp; }
    public Vec3d lockedPos() { return lockedPos; }
    public String applierName() { return applierName; }
    public String targetName() { return targetName; }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("targetUUID", targetUUID.toString());
        nbt.putString("applierUUID", applierUUID.toString());
        nbt.putString("cuffType", cuffType.toNbt());
        nbt.putLong("timestamp", timestamp);
        nbt.putDouble("lockedX", lockedPos.x);
        nbt.putDouble("lockedY", lockedPos.y);
        nbt.putDouble("lockedZ", lockedPos.z);
        nbt.putString("applierName", applierName);
        nbt.putString("targetName", targetName);
        return nbt;
    }

    public static CuffRecord fromNbt(CompoundTag nbt) {
        return new CuffRecord(
            UUID.fromString(nbt.getString("targetUUID")),
            UUID.fromString(nbt.getString("applierUUID")),
            CuffType.fromNbt(nbt.getString("cuffType")),
            nbt.getLong("timestamp"),
            new Vec3d(nbt.getDouble("lockedX"), nbt.getDouble("lockedY"), nbt.getDouble("lockedZ")),
            nbt.getString("applierName"),
            nbt.getString("targetName")
        );
    }
}
