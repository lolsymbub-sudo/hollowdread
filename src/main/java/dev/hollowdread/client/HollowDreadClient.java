package dev.hollowdread.client;

import dev.hollowdread.HollowDread;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment; 
import net.fabricmc.fabric.api.client.render.EntityRendererRegistry;

@Environment(EnvType.CLIENT)
public class HollowDreadClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(HollowDread.WATCHER,
                (dispatcher, context) -> new WatcherRenderer(dispatcher));
    }
}
