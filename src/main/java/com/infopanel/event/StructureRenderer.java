package com.infopanel.event;

import com.infopanel.config.InfoPanelConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class StructureRenderer {

    private static final int RADIUS_CHUNKS = 8;

    // Точные ключи структур
    private static final Map<ResourceKey<Structure>, Integer> EXACT = new LinkedHashMap<>();
    // Префиксы для jigsaw-структур (деревня и т.д.)
    private static final Map<String, Integer> PREFIXES = new LinkedHashMap<>();

    static {
        // Точные ключи
        reg("pillager_outpost", 0xFFFF5555);
        reg("monument",         0xFF5555FF);
        reg("stronghold",       0xFFFFAA00);
        reg("desert_pyramid",   0xFFFFFF55);
        reg("bastion_remnant",  0xFFAA44FF);

        // Jigsaw-структуры — ищем по префиксу
        PREFIXES.put("village",        0xFF55FF55);
        PREFIXES.put("nether_fortress", 0xFFFF6600);
        PREFIXES.put("fortress",        0xFFFF6600); // альтернативный ключ
    }

    private static void reg(String name, int color) {
        EXACT.put(ResourceKey.create(Registries.STRUCTURE,
                ResourceLocation.withDefaultNamespace(name)), color);
    }

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!InfoPanelConfig.isShowStructures()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.getSingleplayerServer() == null) return;

        ServerLevel serverLevel = mc.getSingleplayerServer().getLevel(mc.level.dimension());
        if (serverLevel == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        BlockPos playerPos = mc.player.blockPosition();
        ChunkPos centerChunk = new ChunkPos(playerPos);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        Set<Long> drawn = new HashSet<>();
        var structureRegistry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);

        for (int cx = -RADIUS_CHUNKS; cx <= RADIUS_CHUNKS; cx++) {
            for (int cz = -RADIUS_CHUNKS; cz <= RADIUS_CHUNKS; cz++) {
                ChunkPos chunkPos = new ChunkPos(centerChunk.x + cx, centerChunk.z + cz);
                try {
                    ChunkAccess chunk = serverLevel.getChunk(chunkPos.x, chunkPos.z);
                    Map<Structure, StructureStart> allStarts = chunk.getAllStarts();
                    if (allStarts.isEmpty()) continue;

                    for (Map.Entry<Structure, StructureStart> startEntry : allStarts.entrySet()) {
                        StructureStart start = startEntry.getValue();
                        if (start == null || !start.isValid()) continue;

                        var structureKey = structureRegistry.getResourceKey(startEntry.getKey()).orElse(null);
                        if (structureKey == null) continue;

                        // Определяем цвет — сначала точное совпадение, потом по префиксу
                        Integer color = EXACT.get(structureKey);
                        if (color == null) {
                            String path = structureKey.location().getPath();
                            for (Map.Entry<String, Integer> pEntry : PREFIXES.entrySet()) {
                                if (path.startsWith(pEntry.getKey())) {
                                    color = pEntry.getValue();
                                    break;
                                }
                            }
                        }
                        if (color == null) continue;

                        // Проверяем индивидуальный флаг
                        String path = structureKey.location().getPath();
                        if (path.startsWith("village")      && !InfoPanelConfig.isShowStructVillage())    continue;
                        if (path.equals("pillager_outpost") && !InfoPanelConfig.isShowStructOutpost())    continue;
                        if (path.equals("monument")         && !InfoPanelConfig.isShowStructMonument())   continue;
                        if (path.equals("stronghold")       && !InfoPanelConfig.isShowStructStronghold()) continue;
                        if (path.equals("desert_pyramid")   && !InfoPanelConfig.isShowStructDesert())     continue;
                        if ((path.startsWith("nether_fortress") || path.startsWith("fortress")) && !InfoPanelConfig.isShowStructFortress()) continue;
                        if (path.equals("bastion_remnant")  && !InfoPanelConfig.isShowStructBastion())    continue;

                        var bb = start.getBoundingBox();
                        long dedupKey = ((long)(bb.minX() + 30000000) << 20) ^ (long)(bb.minZ() + 30000000);
                        if (!drawn.add(dedupKey)) continue;

                        float r = ((color >> 16) & 0xFF) / 255f;
                        float g = ((color >> 8)  & 0xFF) / 255f;
                        float b = (color & 0xFF) / 255f;

                        drawBox(poseStack, bufferSource, bb, r, g, b);
                    }
                } catch (Exception ignored) {}
            }
        }

        bufferSource.endBatch();
        poseStack.popPose();
    }

    private void drawBox(PoseStack poseStack, MultiBufferSource bufferSource,
                         net.minecraft.world.level.levelgen.structure.BoundingBox bb,
                         float r, float g, float b) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        float x0 = bb.minX();
        float y0 = bb.minY();
        float z0 = bb.minZ();
        float x1 = bb.maxX() + 1f;
        float y1 = bb.maxY() + 1f;
        float z1 = bb.maxZ() + 1f;
        float a  = 0.9f;

        line(consumer, matrix, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(consumer, matrix, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(consumer, matrix, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(consumer, matrix, x0, y0, z1, x0, y0, z0, r, g, b, a);
        line(consumer, matrix, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(consumer, matrix, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(consumer, matrix, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(consumer, matrix, x0, y1, z1, x0, y1, z0, r, g, b, a);
        line(consumer, matrix, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(consumer, matrix, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(consumer, matrix, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(consumer, matrix, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private void line(VertexConsumer consumer, Matrix4f matrix,
                      float x0, float y0, float z0,
                      float x1, float y1, float z1,
                      float r, float g, float b, float a) {
        float nx = x1 - x0;
        float ny = y1 - y0;
        float nz = z1 - z0;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len == 0) return;
        nx /= len; ny /= len; nz /= len;
        consumer.addVertex(matrix, x0, y0, z0).setColor(r, g, b, a).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz);
    }
}
