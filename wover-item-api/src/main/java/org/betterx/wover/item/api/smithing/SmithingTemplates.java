package org.betterx.wover.item.api.smithing;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.item.api.ItemRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;

import org.spongepowered.include.com.google.common.collect.ImmutableList;

import java.util.List;

public class SmithingTemplates {
    public static final ChatFormatting TITLE_FORMAT = ChatFormatting.GRAY;
    public static final ChatFormatting DESCRIPTION_FORMAT = ChatFormatting.BLUE;

    public static final Identifier EMPTY_SLOT_HELMET = Identifier.withDefaultNamespace("container/slot/helmet");
    public static final Identifier EMPTY_SLOT_CHESTPLATE = Identifier.withDefaultNamespace("container/slot/chestplate");
    public static final Identifier EMPTY_SLOT_LEGGINGS = Identifier.withDefaultNamespace("container/slot/leggings");
    public static final Identifier EMPTY_SLOT_BOOTS = Identifier.withDefaultNamespace("container/slot/boots");
    public static final Identifier EMPTY_SLOT_HOE = Identifier.withDefaultNamespace("container/slot/hoe");
    public static final Identifier EMPTY_SLOT_AXE = Identifier.withDefaultNamespace("container/slot/axe");
    public static final Identifier EMPTY_SLOT_SWORD = Identifier.withDefaultNamespace("container/slot/sword");
    public static final Identifier EMPTY_SLOT_SHOVEL = Identifier.withDefaultNamespace("container/slot/shovel");
    public static final Identifier EMPTY_SLOT_PICKAXE = Identifier.withDefaultNamespace("container/slot/pickaxe");
    public static final Identifier EMPTY_SLOT_INGOT = Identifier.withDefaultNamespace("container/slot/ingot");
    public static final Identifier EMPTY_SLOT_REDSTONE_DUST = Identifier.withDefaultNamespace("container/slot/redstone_dust");
    public static final Identifier EMPTY_SLOT_DIAMOND = Identifier.withDefaultNamespace("container/slot/diamond");

    public static final List<Identifier> TOOLS = List.of(
            EMPTY_SLOT_SWORD,
            EMPTY_SLOT_PICKAXE,
            EMPTY_SLOT_AXE,
            EMPTY_SLOT_HOE,
            EMPTY_SLOT_SHOVEL
    );

    public static final List<Identifier> ARMOR = List.of(
            EMPTY_SLOT_HELMET,
            EMPTY_SLOT_CHESTPLATE,
            EMPTY_SLOT_LEGGINGS,
            EMPTY_SLOT_BOOTS
    );
    public static final List<Identifier> ARMOR_AND_TOOLS = combine(ARMOR, TOOLS);

    public static List<Identifier> combine(List<Identifier>... sources) {
        final ImmutableList.Builder<Identifier> builder = ImmutableList.builder();
        for (var s : sources) {
            builder.addAll(s);
        }
        return builder.build();
    }

    public static Builder create(ModCore modCore, String path) {
        return new Builder(modCore, path);
    }

    public static class Builder {
        private final ModCore C;
        private final String path;
        private List<Identifier> baseSlotEmptyIcons;
        private List<Identifier> additionalSlotEmptyIcons;

        private Builder(ModCore modCore, String path) {
            this.C = modCore;
            this.path = path;
        }

        public Builder setBaseSlotEmptyIcons(List<Identifier> baseSlotEmptyIcons) {
            this.baseSlotEmptyIcons = baseSlotEmptyIcons;
            return this;
        }

        public Builder setAdditionalSlotEmptyIcons(List<Identifier> additionalSlotEmptyIcons) {
            this.additionalSlotEmptyIcons = additionalSlotEmptyIcons;
            return this;
        }

        public SmithingTemplateItem build() {
            if (baseSlotEmptyIcons == null || baseSlotEmptyIcons.isEmpty()) {
                throw new IllegalStateException("Base slot empty icons must contain at least one icon");
            }
            if (additionalSlotEmptyIcons == null || additionalSlotEmptyIcons.isEmpty()) {
                throw new IllegalStateException("Additional slot empty icons must contain at least one icon");
            }

            Item.Properties properties = new Item.Properties();
            var pendingId = ItemRegistry.peekConstructionId();
            if (pendingId != null) {
                properties.setId(pendingId);
            }

            return new SmithingTemplateItem(
                    Component.translatable(Util.makeDescriptionId(
                            "item",
                            Identifier.fromNamespaceAndPath(C.namespace, "smithing_template." + path + ".applies_to")
                    )).withStyle(DESCRIPTION_FORMAT),
                    Component.translatable(Util.makeDescriptionId(
                            "item",
                            Identifier.fromNamespaceAndPath(
                                    C.namespace,
                                    "smithing_template." + path + ".ingredients"
                            )
                    )).withStyle(DESCRIPTION_FORMAT),
                    Component.translatable(Util.makeDescriptionId(
                            "item",
                            Identifier.fromNamespaceAndPath(
                                    C.namespace,
                                    "smithing_template." + path + ".base_slot_description"
                            )
                    )),
                    Component.translatable(Util.makeDescriptionId(
                            "item",
                            Identifier.fromNamespaceAndPath(
                                    C.namespace,
                                    "smithing_template." + path + ".additions_slot_description"
                            )
                    )),
                    baseSlotEmptyIcons,
                    additionalSlotEmptyIcons,
                    properties
            );
        }
    }
}
