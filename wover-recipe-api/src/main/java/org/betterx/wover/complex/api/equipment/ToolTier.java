package org.betterx.wover.complex.api.equipment;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToolTier {

    public static final ToolSlot.PropertiesBuilder DIGGER_ITEM_PROPERTIES = (slot, tier) -> {
        var values = tier.getValues(slot);
        if (values == null)
            throw new IllegalArgumentException("No values for slot " + slot + " in tier " + tier);
        ToolMaterial material = tier.toolTier;
        return switch (slot) {
            case PICKAXE_SLOT -> new Item.Properties().pickaxe(material, values.attackDamage, values.attackSpeed);
            case AXE_SLOT -> new Item.Properties().axe(material, values.attackDamage, values.attackSpeed);
            case SHOVEL_SLOT -> new Item.Properties().shovel(material, values.attackDamage, values.attackSpeed);
            case HOE_SLOT -> new Item.Properties().hoe(material, values.attackDamage, values.attackSpeed);
            case SHEARS_SLOT, HAMMER_SLOT -> new Item.Properties().tool(
                    material,
                    tier.blockTag != null ? tier.blockTag : BlockTags.MINEABLE_WITH_PICKAXE,
                    values.attackDamage,
                    values.attackSpeed,
                    0.0F
            );
            default -> new Item.Properties();
        };
    };

    public static final ToolSlot.PropertiesBuilder SWORD_ITEM_PROPERTIES = (slot, tier) -> {
        var values = tier.getValues(slot);
        if (values == null)
            throw new IllegalArgumentException("No values for slot " + slot + " in tier " + tier);
        return new Item.Properties().sword(tier.toolTier, values.attackDamage, values.attackSpeed);
    };

    public record ToolValues(float attackDamage, float attackSpeed, SmithingTemplateItem smithingTemplate) {
        public ToolValues(float attackDamage, float attackSpeed) {
            this(attackDamage, attackSpeed, null);
        }

        ToolValues copyWithOffset(ToolValues offset) {
            return new ToolValues(
                    attackDamage + offset.attackDamage,
                    attackSpeed + offset.attackSpeed,
                    offset.smithingTemplate
            );
        }
    }

    public final String name;
    public final ToolMaterial toolTier;
    public final TagKey<Block> blockTag;
    private final ToolValues[] toolValues;

    private ToolTier(
            String name,
            ToolMaterial toolTier,
            ToolValues[] toolValues,
            TagKey<Block> blockTag
    ) {
        this.toolTier = toolTier;
        this.toolValues = toolValues;
        this.name = name;
        this.blockTag = blockTag;
    }

    @Nullable
    public ToolValues getValues(ToolSlot slot) {
        return toolValues[slot.slotIndex];
    }

    public static ToolTier.Builder builder(String name) {
        return new ToolTier.Builder(name);
    }

    //a Builder class
    public static class Builder {
        private ToolMaterial toolTier;
        private final ToolValues[] toolValues = new ToolValues[ToolSlot.values().length];
        private final String name;
        private TagKey<Block> blockTag;

        Builder(String name) {
            this.name = name;
        }

        public Builder blockTag(TagKey<Block> blockTag) {
            this.blockTag = blockTag;
            return this;
        }

        public Builder toolTier(ToolMaterial toolTier) {
            this.toolTier = toolTier;
            return this;
        }

        public Builder toolValues(ToolSlot slot, ToolValues toolValues) {
            this.toolValues[slot.slotIndex] = toolValues;
            return this;
        }

        public Builder toolValuesWithOffset(ToolTier source, ToolValues offset) {
            for (int i = 0; i < toolValues.length; i++) {
                if (source.toolValues[i] != null)
                    this.toolValues[i] = source.toolValues[i].copyWithOffset(offset);

            }
            return this;
        }

        public ToolTier build() {
            return new ToolTier(name, toolTier, toolValues, blockTag);
        }
    }

    @Override
    public String toString() {
        return "ToolTier - " + this.name;
    }

    /**
     * Create a new ToolTier for the specified Tier where all Values are offset by the given amount
     *
     * @param newName New Name to use for the copy
     * @param newTier New Tier to use or null if the one from this ToolTier should be used
     * @param offset  Offset to apply to all values
     * @return New ToolTier with the specified offset
     */
    public ToolTier copyWithOffset(
            @NotNull String newName,
            @Nullable ToolMaterial newTier,
            ToolValues offset,
            @Nullable TagKey<Block> blockTag
    ) {
        ToolValues[] newValues = new ToolValues[toolValues.length];
        for (int i = 0; i < toolValues.length; i++) {
            if (toolValues[i] != null)
                newValues[i] = toolValues[i].copyWithOffset(offset);
        }
        return new ToolTier(newName, newTier == null ? this.toolTier : newTier, newValues, blockTag);
    }
}
