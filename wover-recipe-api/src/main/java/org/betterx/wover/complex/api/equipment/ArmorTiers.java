package org.betterx.wover.complex.api.equipment;

import org.betterx.wover.complex.api.equipment.ArmorTier.ArmorValues;

import net.minecraft.world.item.equipment.ArmorMaterials;

public class ArmorTiers {
    public static ArmorTier LEATHER_ARMOR = ArmorTier
            .builder("leather")
            .armorMaterial(ArmorMaterials.LEATHER)
            .allArmorValues(new ArmorValues(5))
            .build();

    public static ArmorTier GOLDEN_ARMOR = ArmorTier
            .builder("golden")
            .armorMaterial(ArmorMaterials.GOLD)
            .allArmorValues(new ArmorValues(7))
            .build();

    public static ArmorTier CHAINMAIL_ARMOR = ArmorTier
            .builder("chainmail")
            .armorMaterial(ArmorMaterials.CHAINMAIL)
            .allArmorValues(new ArmorValues(15))
            .build();

    public static ArmorTier IRON_ARMOR = ArmorTier
            .builder("iron")
            .armorMaterial(ArmorMaterials.IRON)
            .allArmorValues(new ArmorValues(15))
            .build();

    public static ArmorTier DIAMOND_ARMOR = ArmorTier
            .builder("diamond")
            .armorMaterial(ArmorMaterials.DIAMOND)
            .allArmorValues(new ArmorValues(33))
            .build();

    public static ArmorTier NETHERITE_ARMOR = ArmorTier
            .builder("netherite")
            .armorMaterial(ArmorMaterials.NETHERITE)
            .allArmorValues(new ArmorValues(37))
            .build();

    public static ArmorTier TURTLE_ARMOR = ArmorTier
            .builder("turtle")
            .armorMaterial(ArmorMaterials.TURTLE_SCUTE)
            .allArmorValues(new ArmorValues(25))
            .build();
}
