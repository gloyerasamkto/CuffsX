package org.gloyer057.cuffsx.cuff;

public enum CuffType {
    HANDS, LEGS;

    public String toNbt() { return name(); }
    public static CuffType fromNbt(String s) { return valueOf(s); }
}
