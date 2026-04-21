package org.gloyer057.cuffsx.item;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.gloyer057.cuffsx.cuff.CuffType;

public class ModItems {
    public static final HandcuffsItem HANDCUFFS_HANDS = new HandcuffsItem(CuffType.HANDS);
    public static final HandcuffsItem HANDCUFFS_LEGS  = new HandcuffsItem(CuffType.LEGS);

    public static void register() {
        Registry.register(Registry.ITEM, new Identifier("cuffsx", "handcuffs_hands"), HANDCUFFS_HANDS);
        Registry.register(Registry.ITEM, new Identifier("cuffsx", "handcuffs_legs"),  HANDCUFFS_LEGS);
    }
}
