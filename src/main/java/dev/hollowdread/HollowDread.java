package dev.hollowdread;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class HollowDread implements ModInitializer {
    public static final String MOD_ID = "hollowdread";

    public static EntityType<WatcherEntity> WATCHER;

    @Override
    public void onInitialize() {
        WATCHER = Registry.register(
                Registry.ENTITY_TYPE,
                new Identifier(MOD_ID, "watcher"),
                FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, WatcherEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 2.6f))
                        .fireImmune()
                        .trackRangeBlocks(80)
                        .build());

        FabricDefaultAttributeRegistry.register(WATCHER, WatcherEntity.createAttributes());

        Registry.register(
                Registry.ITEM,
                new Identifier(MOD_ID, "watcher_spawn_egg"),
                new SpawnEggItem(WATCHER, 0x0B0B0D, 0xE8E8E8, new Item.Settings().group(ItemGroup.MISC)));

        // Один лёгкий проход раз в секунду на игрока. Больше никаких тиков.
        ServerTickEvents.END_WORLD_TICK.register(HorrorDirector::tick);
    }
}
