package org.betterx.wover.item.api.armor;

import org.betterx.wover.tag.api.TagManager;
import org.betterx.wover.tag.api.event.context.ItemTagBootstrapContext;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public class CustomArmorMaterial {
    private static final List<RepairTagEntry> REPAIR_TAGS = new ArrayList<>();

    static {
        TagManager.ITEMS.bootstrapEvent().subscribe(CustomArmorMaterial::bootstrapRepairTags);
    }

    public static CustomArmorMaterial.Builder start(Identifier location) {
        return new CustomArmorMaterial.Builder(location);
    }

    public static class Builder {
        private final Identifier location;
        private final EnumMap<ArmorType, Integer> defense;
        private int enchantmentValue;
        private Holder<SoundEvent> equipSound;
        private float toughness;
        private float knockbackResistance;
        private int durability = 15;
        private TagKey<Item> repairIngredientTag;
        private Supplier<Ingredient> repairIngredientSupplier;

        private Builder(Identifier location) {
            this.location = location;
            this.defense = new EnumMap<>(ArmorType.class);
        }

        public Builder defense(int boots, int leggings, int chestplate, int helmet, int body) {
            defense(ArmorType.BOOTS, boots);
            defense(ArmorType.LEGGINGS, leggings);
            defense(ArmorType.CHESTPLATE, chestplate);
            defense(ArmorType.HELMET, helmet);
            defense(ArmorType.BODY, body);
            return this;
        }

        public Builder defense(ArmorType type, int defense) {
            this.defense.put(type, defense);
            return this;
        }

        public Builder enchantmentValue(int enchantmentValue) {
            this.enchantmentValue = enchantmentValue;
            return this;
        }

        public Builder equipSound(Holder<SoundEvent> equipSound) {
            this.equipSound = equipSound;
            return this;
        }

        public Builder toughness(float toughness) {
            this.toughness = toughness;
            return this;
        }

        public Builder knockbackResistance(float knockbackResistance) {
            this.knockbackResistance = knockbackResistance;
            return this;
        }

        public Builder durability(int durability) {
            this.durability = durability;
            return this;
        }

        public Builder repairIngredientTag(TagKey<Item> repairIngredientTag) {
            this.repairIngredientTag = repairIngredientTag;
            return this;
        }

        public Builder repairIngredientSupplier(Supplier<Ingredient> repairIngredientSupplier) {
            this.repairIngredientSupplier = repairIngredientSupplier;
            return this;
        }

        protected void validate() throws IllegalStateException {
            if (defense.size() != ArmorType.values().length) {
                throw new IllegalStateException("Defense values must be set for all armor types");
            }

            if (enchantmentValue < 0) {
                throw new IllegalStateException("Enchantment value must be non-negative");
            }

            if (equipSound == null) {
                throw new IllegalStateException("Equip sound must be set");
            }

            if (toughness < 0) {
                throw new IllegalStateException("Toughness must be non-negative");
            }

            if (knockbackResistance < 0) {
                throw new IllegalStateException("Knockback resistance must be non-negative");
            }

            if (durability <= 0) {
                throw new IllegalStateException("Durability must be positive");
            }

            if (repairIngredientTag == null && repairIngredientSupplier == null) {
                throw new IllegalStateException("Repair ingredient supplier must be set");
            }
        }

        public ArmorMaterial build() {
            validate();
            TagKey<Item> repairTag = resolveRepairTag();
            ResourceKey<EquipmentAsset> assetId = ResourceKey.create(EquipmentAssets.ROOT_ID, location);
            return new ArmorMaterial(
                    durability,
                    defense,
                    enchantmentValue,
                    equipSound,
                    toughness,
                    knockbackResistance,
                    repairTag,
                    assetId
            );
        }

        public Holder<ArmorMaterial> buildAndRegister() {
            return Holder.direct(this.build());
        }

        private TagKey<Item> resolveRepairTag() {
            if (repairIngredientTag != null) return repairIngredientTag;
            Identifier tagId = Identifier.fromNamespaceAndPath(location.getNamespace(), "repair/" + location.getPath());
            TagKey<Item> tag = TagManager.ITEMS.makeTag(tagId);
            REPAIR_TAGS.add(new RepairTagEntry(tag, repairIngredientSupplier));
            return tag;
        }
    }

    private static void bootstrapRepairTags(ItemTagBootstrapContext ctx) {
        for (RepairTagEntry entry : REPAIR_TAGS) {
            entry.addTo(ctx);
        }
    }

    private static final class RepairTagEntry {
        private final TagKey<Item> tag;
        private final Supplier<Ingredient> supplier;

        private RepairTagEntry(TagKey<Item> tag, Supplier<Ingredient> supplier) {
            this.tag = tag;
            this.supplier = supplier;
        }

        private void addTo(ItemTagBootstrapContext ctx) {
            if (supplier == null) return;
            Ingredient ingredient = supplier.get();
            if (ingredient == null) return;
            ingredient.items()
                      .map(holder -> holder.value())
                      .forEach(item -> ctx.add(tag, item));
        }
    }
}
