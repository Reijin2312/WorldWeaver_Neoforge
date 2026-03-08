package org.betterx.wover.testmod.recipe;

import org.betterx.wover.complex.api.equipment.*;
import org.betterx.wover.testmod.entrypoint.TestModWoverRecipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.ApiStatus;

public class TestEquipmentSet extends EquipmentSet {
    public static final TestEquipmentSet INSTANCE = new TestEquipmentSet();

    public TestEquipmentSet() {
        super(TestModWoverRecipe.C, "test_equipment_set", ToolTiers.DIAMOND_TOOL, ArmorTiers.TURTLE_ARMOR, Items.STONE);

        add(ToolSlot.PICKAXE_SLOT, (m, p) -> new Item(p));
        add(ToolSlot.AXE_SLOT, (m, p) -> new Item(p));
        add(ToolSlot.SHOVEL_SLOT, (m, p) -> new Item(p));
        add(ToolSlot.HOE_SLOT, (m, p) -> new Item(p));
        add(ToolSlot.SWORD_SLOT, (m, p) -> new Item(p));

        add(ArmorSlot.HELMET_SLOT, (m, t, p) -> new Item(p));
        add(ArmorSlot.CHESTPLATE_SLOT, (m, t, p) -> new Item(p));
        add(ArmorSlot.LEGGINGS_SLOT, (m, t, p) -> new Item(p));
        add(ArmorSlot.BOOTS_SLOT, (m, t, p) -> new Item(p));
    }

    @ApiStatus.Internal
    public static void ensureStaticInit() {
        // NO-OP
    }
}
