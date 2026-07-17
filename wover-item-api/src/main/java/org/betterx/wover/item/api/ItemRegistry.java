package org.betterx.wover.item.api;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.item.api.smithing.SmithingTemplates;
import org.betterx.wover.tag.api.event.context.ItemTagBootstrapContext;

import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.DispenserBlock;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class ItemRegistry {
    private static final Map<ModCore, ItemRegistry> REGISTRIES = new HashMap<>();
    public final ModCore C;
    private final Map<ResourceLocation, Item> items = new LinkedHashMap<>();
    private Map<Item, TagKey<Item>[]> datagenTags;

    private ItemRegistry(ModCore modeCore) {
        this.C = modeCore;

        if (ModCore.isDatagen()) {
            datagenTags = new HashMap<>();
        }
    }

    public static Stream<ItemRegistry> streamAll() {
        return REGISTRIES.values().stream();
    }

    public static ItemRegistry forMod(ModCore modCore) {
        return REGISTRIES.computeIfAbsent(modCore, c -> new ItemRegistry(modCore));
    }

    public Stream<Item> allItems() {
        return items.values().stream();
    }

    public <T extends Item> T register(String path, T item, TagKey<Item>... tags) {
        if (item != null && item != Items.AIR) {
            ResourceLocation id = C.mk(path);
            Registry.register(BuiltInRegistries.ITEM, id, item);
            items.put(id, item);

            if (datagenTags != null && tags != null && tags.length > 0) datagenTags.put(item, tags);
        }

        return item;
    }

    public <T extends Item> T registerAsTool(String path, T item, TagKey<Item>... tags) {
        register(path, item, tags);

        return item;
    }

    public FoodProperties.Builder foodPropertiesOf(int hunger, float saturation, MobEffectInstance... effects) {
        FoodProperties.Builder builder = new FoodProperties.Builder().nutrition(hunger).saturationModifier(saturation);
        for (MobEffectInstance effect : effects) {
            builder.effect(effect, 1F);
        }
        return builder;
    }

    public FoodProperties.Builder drinkPropertiesOf(int hunger, float saturation) {
        return new FoodProperties.Builder().nutrition(hunger).saturationModifier(saturation);
    }


    public <T extends Item> T registerFood(
            String name, Function<Item.Properties, T> factory,
            int hunger, float saturation,
            MobEffectInstance... effects
    ) {
        return registerFood(name, factory, createDefaultItemSettings(), hunger, saturation, effects);
    }

    public <T extends Item> T registerFood(
            String name, Function<Item.Properties, T> factory, Item.Properties properties,
            int hunger, float saturation,
            MobEffectInstance... effects
    ) {
        return this.register(name, factory.apply(properties.food(this
                .foodPropertiesOf(hunger, saturation, effects)
                .build())));
    }
    
    public <T extends Item> T registerDrink(
            String name, Function<Item.Properties, T> factory,
            int hunger, float saturation,
            MobEffectInstance... effects
    ) {
        return registerDrink(name, factory, createDefaultItemSettings(), hunger, saturation, effects);
    }


    public <T extends Item> T registerDrink(
            String name, Function<Item.Properties, T> factory, Item.Properties properties,
            int hunger, float saturation,
            MobEffectInstance... effects
    ) {
        return this.register(name, factory.apply(properties.food(this
                .drinkPropertiesOf(hunger, saturation)
                .build())));
    }


    public <T extends SpawnEggItem> T registerEgg(String path, T item, TagKey<Item>... tags) {
        DefaultDispenseItemBehavior behavior = new DefaultDispenseItemBehavior() {
            public ItemStack execute(BlockSource pointer, ItemStack stack) {
                Direction direction = pointer.state().getValue(DispenserBlock.FACING);
                EntityType<?> entityType = ((SpawnEggItem) stack.getItem()).getType(stack);
                entityType.spawn(
                        pointer.level(),
                        stack,
                        null,
                        pointer.pos().relative(direction),
                        MobSpawnType.DISPENSER,
                        direction != Direction.UP,
                        false
                );
                stack.shrink(1);
                return stack;
            }
        };
        DispenserBlock.registerBehavior(item, behavior);
        return register(path, item, tags);
    }

    public SmithingTemplateItem registerSmithingTemplateItem(
            String path,
            List<ResourceLocation> baseSlotEmptyIcons,
            List<ResourceLocation> additionalSlotEmptyIcons
    ) {
        final SmithingTemplateItem item = SmithingTemplates
                .create(C, path)
                .setBaseSlotEmptyIcons(baseSlotEmptyIcons)
                .setAdditionalSlotEmptyIcons(additionalSlotEmptyIcons)
                .build();

        return registerSmithingTemplateItem(path + "_smithing_template", item);
    }

    public <T extends SmithingTemplateItem> T registerSmithingTemplateItem(
            String path,
            T item
    ) {
        register(path, item);
        return item;
    }

    public Item.Properties createDefaultItemSettings() {
        return new Item.Properties();
    }

    public void bootstrapItemTags(ItemTagBootstrapContext ctx) {
        if (datagenTags != null) {
            datagenTags.forEach(ctx::add);
        }
        items
                .entrySet()
                .stream()
                .filter(i -> i.getValue() instanceof ItemTagProvider)
                .forEach(i -> ((ItemTagProvider) i.getValue()).registerItemTags(i.getKey(), ctx));
    }
}
