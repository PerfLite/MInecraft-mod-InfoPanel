package com.infopanel.event;

import com.infopanel.config.InfoPanelConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Random;

public class SlimeChunkRenderer {

    private static final int CHUNK_RADIUS = 4;
    private static final float R = 0.0f, G = 1.0f, B = 0.0f, A = 0.25f;

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!InfoPanelConfig.isShowSlimeChunks()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.level.dimension().equals(Level.OVERWORLD)) return;

        long seed;
        if (mc.getSingleplayerServer() != null) {
            seed = mc.getSingleplayerServer().getWorldData().worldGenOptions().seed();
        } else {
            return;
        }

        Camera camera = event.getCamera();
        net.minecraft.world.phys.Vec3 camPos = camera.getPosition();

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        int playerChunkX = mc.player.chunkPosition().x;
        int playerChunkZ = mc.player.chunkPosition().z;
        int playerY = mc.player.blockPosition().getY();

        for (int cx = playerChunkX - CHUNK_RADIUS; cx <= playerChunkX + CHUNK_RADIUS; cx++) {
            for (int cz = playerChunkZ - CHUNK_RADIUS; cz <= playerChunkZ + CHUNK_RADIUS; cz++) {
                if (isSlimeChunk(seed, cx, cz)) {
                    drawChunkOverlay(poseStack, bufferSource, cx, cz, playerY);
                }
            }
        }

        bufferSource.endBatch();
        poseStack.popPose();
    }

    private boolean isSlimeChunk(long worldSeed, int chunkX, int chunkZ) {
        Random rng = new Random(
            worldSeed
            + (long)(chunkX * chunkX * 0x4c1906)
            + (long)(chunkX * 0x5ac0db)
            + (long)(chunkZ * chunkZ) * 0x4307a7L
            + (long)(chunkZ * 0x5f24f)
            ^ 0x3ad8025f
        );
        return rng.nextInt(10) == 0;
    }

    private void drawChunkOverlay(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                   int chunkX, int chunkZ, int y) {
        Matrix4f matrix = poseStack.last().pose();
        float x0 = chunkX * 16;
        float x1 = x0 + 16;
        float z0 = chunkZ * 16;
        float z1 = z0 + 16;
        float fy = y + 0.05f;

        // Заливка через lines (4 линии по периметру) + диагонали для визуального эффекта
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());

        // Контур чанка
        addLine(matrix, lines, x0, fy, z0, x1, fy, z0);
        addLine(matrix, lines, x1, fy, z0, x1, fy, z1);
        addLine(matrix, lines, x1, fy, z1, x0, fy, z1);
        addLine(matrix, lines, x0, fy, z1, x0, fy, z0);

        // Сетка внутри чанка (каждые 4 блока)
        for (int i = 4; i < 16; i += 4) {
            addLine(matrix, lines, x0 + i, fy, z0, x0 + i, fy, z1);
            addLine(matrix, lines, x0, fy, z0 + i, x1, fy, z0 + i);
        }
    }

    private void addLine(Matrix4f matrix, VertexConsumer consumer,
                         float x0, float y0, float z0,
                         float x1, float y1, float z1) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len == 0) return;
        consumer.addVertex(matrix, x0, y0, z0).setColor(R, G, B, 0.8f).setNormal(dx/len, dy/len, dz/len);
        consumer.addVertex(matrix, x1, y1, z1).setColor(R, G, B, 0.8f).setNormal(dx/len, dy/len, dz/len);
    }
}
