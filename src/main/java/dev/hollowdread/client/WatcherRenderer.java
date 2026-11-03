package dev.hollowdread.client;

import dev.hollowdread.HollowDread;
import dev.hollowdread.WatcherEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class WatcherRenderer extends MobEntityRenderer<WatcherEntity, BipedEntityModel<WatcherEntity>> {
    private static final Identifier TEXTURE = new Identifier(HollowDread.MOD_ID, "textures/entity/watcher.png");

    public WatcherRenderer(EntityRenderDispatcher dispatcher) {
        super(dispatcher, new BipedEntityModel<>(0.0f), 0.0f); // без тени
    }

    @Override
    public Identifier getTexture(WatcherEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(WatcherEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(0.95f, 1.45f, 0.95f); // вытянутый, неправильный силуэт
    }
}
