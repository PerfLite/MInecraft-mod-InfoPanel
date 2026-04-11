package com.infopanel.event;

import com.infopanel.config.InfoPanelConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

public class LightOverlayRenderer {

    private static final int RADIUS = 16;
    private static final int COLOR_SAFE   = 0xFF55FF55; // зелёный
    private static final int COLOR_DANGER = 0xFFFF5555; // красный

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!InfoPanelConfig.isShowLightOverlay()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Level level = mc.level;
        BlockPos center = mc.player.blockPosition();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                // Ищем верхний твёрдый блок в колонке
                BlockPos surface = getSurface(level, center.offset(dx, 0, dz));
                if (surface == null) continue;

                int blockLight = level.getBrightness(LightLayer.BLOCK, surface);
                boolean danger = blockLight <= 7;

                int color = danger ? COLOR_DANGER : COLOR_SAFE;
                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >> 8)  & 0xFF) / 255f;
                float b = (color & 0xFF) / 255f;

                // Рисуем крестик на поверхности блока
                drawCross(poseStack, bufferSource, surface, r, g, b);
            }
        }

        bufferSource.endBatch();
        poseStack.popPose();
    }

    private BlockPos getSurface(Level level, BlockPos origin) {
        // Ищем вниз от игрока (до 10 блоков)
        BlockPos pos = new BlockPos(origin.getX(), origin.getY() + 1, origin.getZ());
        for (int i = 0; i < 15; i++) {
            BlockPos below = pos.below();
            BlockState stateBelow = level.getBlockState(below);
            BlockState stateAt    = level.getBlockState(pos);
            if (stateBelow.isSolidRender(level, below) && stateAt.isAir()) {
                return pos; // pos — блок воздуха над твёрдым блоком
            }
            pos = below;
        }
        return null;
    }

    private void drawCross(PoseStack poseStack, MultiBufferSource bufferSource,
                           BlockPos pos, float r, float g, float b) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        float x = pos.getX() + 0.5f;
        float y = pos.getY() + 0.01f; // чуть выше поверхности
        float z = pos.getZ() + 0.5f;
        float size = 0.3f;

        // Линия X
        consumer.addVertex(matrix, x - size, y, z).setColor(r, g, b, 1f).setNormal(1, 0, 0);
        consumer.addVertex(matrix, x + size, y, z).setColor(r, g, b, 1f).setNormal(1, 0, 0);
        // Линия Z
        consumer.addVertex(matrix, x, y, z - size).setColor(r, g, b, 1f).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x, y, z + size).setColor(r, g, b, 1f).setNormal(0, 0, 1);
    }
}
