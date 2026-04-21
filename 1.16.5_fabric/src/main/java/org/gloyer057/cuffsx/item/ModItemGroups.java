package org.gloyer057.cuffsx.item;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup CUFFSX_GROUP = FabricItemGroupBuilder.build(
        new Identifier("cuffsx", "cuffsx"),
        () -> new ItemStack(ModItems.HANDCUFFS_HANDS)
    );
}
