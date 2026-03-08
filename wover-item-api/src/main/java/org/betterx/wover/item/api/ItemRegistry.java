package org.betterx.wover.item.api;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.item.api.smithing.SmithingTemplates;
import org.betterx.wover.tag.api.event.context.ItemTagBootstrapContext;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.DispenserBlock;
import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicBoolean;

import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.bus.api.IEventBus;

public class ItemRegistry {
    private static final Map<ModCore, ItemRegistry> REGISTRIES = new HashMap<>();
    private static final AtomicBoolean HOOKED = new AtomicBoolean(false);
    private static final ThreadLocal<ArrayDeque<ResourceKey<Item>>> CONSTRUCTION_IDS = ThreadLocal.withInitial(ArrayDeque::new);
    public final ModCore C;
    private final Map<Identifier, Item> items = new LinkedHashMap<>();
    private Map<Item, TagKey<Item>[]> datagenTags;
    private Runnable initializer;
    private boolean initialized;

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

    public void setInitializer(Runnable initializer) {
        this.initializer = initializer;
    }

    private void ensureInitialized() {
        if (!initialized && initializer != null) {
            initialized = true;
            initializer.run();
        }
    }

    public <T extends Item> T register(String path, T item, TagKey<Item>... tags) {
        if (item != null && item != Items.AIR) {
            Identifier id = C.mk(path);
            ensureIntrusiveHolder(item);
            items.put(id, item);

            if (datagenTags != null && tags != null && tags.length > 0) datagenTags.put(item, tags);
        }

        return item;
    }

    private void performRegistration(RegisterEvent.RegisterHelper<Item> helper) {
        ensureInitialized();
        items.forEach((id, item) -> {
            ensureIntrusiveHolder(item);
            helper.register(id, item);
        });
    }

    private static void ensureIntrusiveHolder(Item item) {
        BuiltInRegistries.ITEM.createIntrusiveHolder(item);
    }

    public static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.ITEM)) {
            event.register(Registries.ITEM, helper -> REGISTRIES.values().forEach(reg -> reg.performRegistration(helper)));
        }
    }

    public static void hook(IEventBus bus) {
        if (HOOKED.compareAndSet(false, true)) {
            bus.addListener(RegisterEvent.class, ItemRegistry::onRegister);
        }
    }

    public String prepareConstructionPath(String path) {
        pushConstructionId(C.mk(path));
        return path;
    }

    public void finishConstructionPath() {
        popConstructionId();
    }

    public static void pushConstructionId(Identifier id) {
        CONSTRUCTION_IDS.get().push(ResourceKey.create(Registries.ITEM, id));
    }

    public static void popConstructionId() {
        ArrayDeque<ResourceKey<Item>> stack = CONSTRUCTION_IDS.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static ResourceKey<Item> peekConstructionId() {
        return CONSTRUCTION_IDS.get().peek();
    }

    public static ResourceKey<Item> takeConstructionId() {
        ArrayDeque<ResourceKey<Item>> stack = CONSTRUCTION_IDS.get();
        return stack.isEmpty() ? null : stack.pop();
    }

    /**
     * Classloader-safe accessor for mixin-injected Minecraft code.
     */
    public static String takeConstructionIdString() {
        ResourceKey<Item> key = takeConstructionId();
        return key == null ? null : key.identifier().toString();
    }

    public static <T> T withConstructionId(Identifier id, Supplier<T> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        ArrayDeque<ResourceKey<Item>> stack = CONSTRUCTION_IDS.get();
        stack.push(key);
        try {
            return factory.get();
        } finally {
            if (!stack.isEmpty() && key.equals(stack.peek())) {
                stack.pop();
            }
        }
    }

    public <T extends Item> T registerAsTool(String path, T item, TagKey<Item>... tags) {
        register(path, item, tags);

        return item;
    }

    public FoodProperties.Builder foodPropertiesOf(int hunger, float saturation, MobEffectInstance... effects) {
        return new FoodProperties.Builder().nutrition(hunger).saturationModifier(saturation);
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
        Item.Properties props = applyFoodProperties(properties, hunger, saturation, effects);
        return this.register(name, factory.apply(props));
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
        Item.Properties props = applyFoodProperties(properties, hunger, saturation, effects);
        return this.register(name, factory.apply(props));
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
                        EntitySpawnReason.DISPENSER,
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
            List<Identifier> baseSlotEmptyIcons,
            List<Identifier> additionalSlotEmptyIcons
    ) {
        final String itemPath = path + "_smithing_template";
        final SmithingTemplateItem item = withConstructionId(C.mk(itemPath), () -> SmithingTemplates
                .create(C, path)
                .setBaseSlotEmptyIcons(baseSlotEmptyIcons)
                .setAdditionalSlotEmptyIcons(additionalSlotEmptyIcons)
                .build());

        return registerSmithingTemplateItem(itemPath, item);
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

    private static Item.Properties applyFoodProperties(
            Item.Properties properties,
            int hunger,
            float saturation,
            MobEffectInstance... effects
    ) {
        FoodProperties food = new FoodProperties.Builder()
                .nutrition(hunger)
                .saturationModifier(saturation)
                .build();
        Consumable consumable = buildConsumable(effects);
        if (consumable != null) {
            return properties.food(food, consumable);
        }
        return properties.food(food);
    }

    private static Consumable buildConsumable(MobEffectInstance... effects) {
        if (effects == null || effects.length == 0) return null;
        Consumable.Builder builder = Consumable.builder();
        for (MobEffectInstance effect : effects) {
            builder.onConsume(new ApplyStatusEffectsConsumeEffect(effect, 1.0F));
        }
        return builder.build();
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
