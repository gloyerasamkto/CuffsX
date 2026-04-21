package org.gloyer057.cuffsx.item;

import net.minecraft.item.Item;
import org.gloyer057.cuffsx.cuff.CuffType;

public class HandcuffsItem extends Item {
    private final CuffType cuffType;

    public HandcuffsItem(CuffType cuffType) {
        super(new Item.Settings().maxCount(1).group(ModItemGroups.CUFFSX_GROUP));
        this.cuffType = cuffType;
    }

    public CuffType getCuffType() { return cuffType; }
}
