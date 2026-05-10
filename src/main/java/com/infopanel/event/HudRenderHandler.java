package com.infopanel.event;

import com.infopanel.client.KeyBindings;
import com.infopanel.client.InfoPanelConfigScreen;
import com.infopanel.client.PanelEditScreen;
import com.infopanel.config.InfoPanelConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class HudRenderHandler {

    private boolean hudVisible_unused; // удалено — используем InfoPanelConfig.hudVisible

    // Размер панели — обновляется при рендере (для будущего использования)
    private int lastPanelX = 0;
    private int lastPanelY = 0;
    private int lastPanelW = 0;
    private int lastPanelH = 0;

    // TPS — считаем по серверным тикам
    private static long lastTickTime = System.currentTimeMillis();
    private static int tickCount = 0;
    private static float cachedTps = 20.0f;

    private static final net.minecraft.resources.ResourceLocation TEX_SUN  =
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("infopanel", "textures/icon_sun.png");
    private static final net.minecraft.resources.ResourceLocation TEX_MOON =
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("infopanel", "textures/icon_moon.png");

    // Размер иконок в пикселях (ширина = высота PNG файла)
    private static final int ICON_TEX_SIZE = 16;

    // icon: null = нет иконки, иначе ResourceLocation текстуры
    private record HudLine(String text, int color, net.minecraft.resources.ResourceLocation icon) {
        HudLine(String text, int color) { this(text, color, null); }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if (KeyBindings.TOGGLE_HUD.consumeClick()) InfoPanelConfig.hudVisible = !InfoPanelConfig.hudVisible;
        if (KeyBindings.TOGGLE_LIGHT_OVERLAY.consumeClick())
            InfoPanelConfig.setShowLightOverlay(!InfoPanelConfig.isShowLightOverlay());
        if (KeyBindings.TOGGLE_SLIME_CHUNKS.consumeClick())
            InfoPanelConfig.setShowSlimeChunks(!InfoPanelConfig.isShowSlimeChunks());
        if (KeyBindings.OPEN_CONFIG.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof InfoPanelConfigScreen) {
                // Уже открыт — закрываем
                InfoPanelConfig.save();
                mc.setScreen(null);
            } else {
                mc.setScreen(new InfoPanelConfigScreen(mc.screen));
            }
        }
        if (KeyBindings.EDIT_LAYOUT.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new PanelEditScreen());
        }
    }

    @SubscribeEvent
    public void onRenderHud(RenderGuiEvent.Post event) {
        if (!InfoPanelConfig.hudVisible) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        // Скрываем только при полноэкранных меню (пауза, конфиг и т.д.)
        // При чате, инвентаре и прочем — показываем
        if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)
                && !(mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;
        if (mc.options.hideGui) return;
        if (mc.player.isSpectator()) return;

        Player player = mc.player;
        GuiGraphics graphics = event.getGuiGraphics();
        BlockPos pos = player.blockPosition();

        List<HudLine> lines = new ArrayList<>();

        if (InfoPanelConfig.isShowCoords())
            lines.add(new HudLine(
                String.format("X: %.1f  Y: %.1f  Z: %.1f", player.getX(), player.getY(), player.getZ()),
                InfoPanelConfig.colorCoords
            ));

        if (InfoPanelConfig.isShowDirection())
            lines.add(new HudLine(getDirection(player.getYRot()), InfoPanelConfig.colorDirection));

        if (InfoPanelConfig.isShowBiome())
            lines.add(new HudLine((Lang.isRussian() ? "Биом: " : "Biome: ") + getBiomeName(mc, pos), InfoPanelConfig.colorBiome));

        if (InfoPanelConfig.isShowFps()) {
            int fps = mc.getFps();
            int color = fps >= 60 ? 0x55FF55 : fps >= 30 ? 0xFF9900 : 0xFF5555;
            lines.add(new HudLine("FPS: " + fps, color));
        }

        if (InfoPanelConfig.isShowPing()) {
            int ping = getPing(mc);
            int color = ping < 80 ? 0x55FF55 : ping < 150 ? 0xFF9900 : 0xFF5555;
            String pingStr = ping < 0 ? "—" : ping + (Lang.isRussian() ? " мс" : " ms");
            lines.add(new HudLine((Lang.isRussian() ? "Пинг: " : "Ping: ") + pingStr, color));
        }

        if (InfoPanelConfig.isShowTps()) {
            float tps = getTps(mc);
            int color = tps >= 19 ? 0x55FF55 : tps >= 15 ? 0xFF9900 : 0xFF5555;
            lines.add(new HudLine(String.format("TPS: %.1f", tps), color));
        }

        if (InfoPanelConfig.isShowTime()) {
            long dayTime = mc.level.getDayTime() % 24000;
            boolean isNight = dayTime >= 13000;
            lines.add(new HudLine(getGameTime(mc), InfoPanelConfig.colorTime,
                    isNight ? TEX_MOON : TEX_SUN));
        }

        if (InfoPanelConfig.isShowSession()) {
            lines.add(new HudLine(getSessionTime(), InfoPanelConfig.colorSession));
        }

        if (InfoPanelConfig.isShowPlayers()) {
            int count = getPlayerCount(mc);
            lines.add(new HudLine((Lang.isRussian() ? "Игроки: " : "Players: ") + count, InfoPanelConfig.colorPlayers));
        }

        // Прочность рисуется отдельно от основной панели
        if (InfoPanelConfig.isShowDurability()) {
            renderDurability(graphics, player, mc);
        }

        float scale     = InfoPanelConfig.getScale();
        int padding     = 3;
        int lineH       = (int)(11 * scale);
        int totalHeight = lineH * lines.size();

        // Считаем ширину с учётом иконок (иконка после текста: textW + GAP + ICON_RAW)
        final int ICON_RAW = 10;
        final int ICON_GAP = 2;
        int maxRawWidth = 0;
        for (HudLine line : lines) {
            int w = mc.font.width(line.text());
            if (line.icon() != null) w += ICON_GAP + ICON_RAW;
            if (w > maxRawWidth) maxRawWidth = w;
        }
        int scaledWidth = (int)(maxRawWidth * scale);

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int startX, startY;
        // Если позиция задана перетаскиванием — используем её
        if (InfoPanelConfig.getPanelX() >= 0 && InfoPanelConfig.getPanelY() >= 0) {
            startX = InfoPanelConfig.getPanelX();
            startY = InfoPanelConfig.getPanelY();
        } else {
            switch (InfoPanelConfig.getPosition()) {
                case TOP_RIGHT    -> { startX = screenW - scaledWidth - padding * 2; startY = 0; }
                case BOTTOM_LEFT  -> { startX = 0; startY = screenH - totalHeight - padding * 2 - 60; }
                case BOTTOM_RIGHT -> { startX = screenW - scaledWidth - padding * 2; startY = screenH - totalHeight - padding * 2 - 60; }
                default           -> { startX = 0; startY = 0; }
            }
        }

        // Сохраняем bounds для PanelEditScreen
        lastPanelX = startX;
        lastPanelY = startY;
        lastPanelW = scaledWidth + padding * 2;
        lastPanelH = totalHeight + padding * 2;
        PanelEditScreen.panelW = lastPanelW;
        PanelEditScreen.panelH = lastPanelH;

        graphics.fill(
            startX, startY,
            startX + scaledWidth + padding * 2,
            startY + totalHeight + padding * 2,
            (InfoPanelConfig.getBgAlpha() << 24) | 0x000000
        );

        graphics.pose().pushPose();
        graphics.pose().translate(startX + padding, startY + padding, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        for (int i = 0; i < lines.size(); i++) {
            HudLine line = lines.get(i);
            int color = 0xFF000000 | line.color();
            int textX = 0;
            int lineY = i * 11;

            if (line.icon() != null) {
                // Сначала рисуем текст
                graphics.drawString(mc.font, line.text(), 0, lineY, color, true);

                // Иконка после текста: отступ = ширина текста + GAP
                int textWidth = mc.font.width(line.text());
                int iconX = textWidth + ICON_GAP;
                // Выравниваем иконку по базовой линии текста: шрифт рисуется с Y=lineY,
                // высота глифа ~7px, иконка 10px — сдвигаем на -1 чтобы верх совпал
                int iconY = lineY - 1;

                com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, line.icon());
                com.mojang.blaze3d.systems.RenderSystem.setShader(
                    net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

                org.joml.Matrix4f mat = graphics.pose().last().pose();
                com.mojang.blaze3d.vertex.BufferBuilder buf =
                    com.mojang.blaze3d.vertex.Tesselator.getInstance()
                        .begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                               com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX);
                float x0 = iconX, y0 = iconY, x1 = iconX + ICON_RAW, y1 = iconY + ICON_RAW;
                buf.addVertex(mat, x0, y0, 0).setUv(0f, 0f);
                buf.addVertex(mat, x0, y1, 0).setUv(0f, 1f);
                buf.addVertex(mat, x1, y1, 0).setUv(1f, 1f);
                buf.addVertex(mat, x1, y0, 0).setUv(1f, 0f);
                com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buf.buildOrThrow());

                com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            } else {
                graphics.drawString(mc.font, line.text(), textX, lineY, color, true);
            }
        }
        graphics.pose().popPose();
    }

    private String getTargetBlock(Minecraft mc) {
        try {
            var hit = mc.hitResult;
            if (hit instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                BlockPos targetPos = blockHit.getBlockPos();
                var state = mc.level.getBlockState(targetPos);
                if (state.isAir()) return null;
                return state.getBlock().getName().getString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private int getPlayerCount(Minecraft mc) {
        try {
            var conn = mc.getConnection();
            if (conn == null) return 1;
            return conn.getOnlinePlayers().size();
        } catch (Exception e) { return 1; }
    }

    private int getPing(Minecraft mc) {
        try {
            ClientPacketListener conn = mc.getConnection();
            if (conn == null) return -1;
            var info = conn.getPlayerInfo(mc.player.getUUID());
            return info != null ? info.getLatency() : -1;
        } catch (Exception e) { return -1; }
    }

    private float getTps(Minecraft mc) {
        try {
            // На клиенте TPS получаем через среднее время тика сервера если доступно
            if (mc.getSingleplayerServer() != null) {
                long[] times = mc.getSingleplayerServer().getTickTimesNanos();
                if (times != null && times.length > 0) {
                    long avg = 0;
                    for (long t : times) avg += t;
                    avg /= times.length;
                    float mspt = avg / 1_000_000f;
                    return Math.min(20f, 1000f / Math.max(mspt, 1f));
                }
            }
        } catch (Exception ignored) {}
        return 20.0f;
    }

    private static final long SESSION_START = System.currentTimeMillis();

    private String getSessionTime() {
        long elapsed = (System.currentTimeMillis() - SESSION_START) / 1000;
        long h = elapsed / 3600;
        long m = (elapsed % 3600) / 60;
        long s = elapsed % 60;
        String label = Lang.isRussian() ? "Сессия: " : "Session: ";
        if (h > 0)
            return String.format(label + "%d:%02d:%02d", h, m, s);
        return String.format(label + "%02d:%02d", m, s);
    }

    private String getGameTime(Minecraft mc) {
        long time = mc.level.getDayTime() % 24000;
        long hours = (time / 1000 + 6) % 24;
        long minutes = (time % 1000) * 60 / 1000;
        String label = Lang.isRussian() ? "Время: " : "Time: ";
        return String.format(label + "%02d:%02d", hours, minutes);
    }

    private String getDirection(float yRot) {
        float n = ((yRot % 360) + 360) % 360;
        if (Lang.isRussian()) {
            if (n < 22.5 || n >= 337.5) return "Юг (-Z)";
            if (n < 67.5)  return "ЮЗ (-X-Z)";
            if (n < 112.5) return "Запад (-X)";
            if (n < 157.5) return "СЗ (-X+Z)";
            if (n < 202.5) return "Север (+Z)";
            if (n < 247.5) return "СВ (+X+Z)";
            if (n < 292.5) return "Восток (+X)";
            return "ЮВ (+X-Z)";
        } else {
            if (n < 22.5 || n >= 337.5) return "South (-Z)";
            if (n < 67.5)  return "SW (-X-Z)";
            if (n < 112.5) return "West (-X)";
            if (n < 157.5) return "NW (-X+Z)";
            if (n < 202.5) return "North (+Z)";
            if (n < 247.5) return "NE (+X+Z)";
            if (n < 292.5) return "East (+X)";
            return "SE (+X-Z)";
        }
    }

    private String getBiomeName(Minecraft mc, BlockPos pos) {
        try {
            String unknown = Lang.isRussian() ? "Неизвестно" : "Unknown";
            return mc.level.getBiome(pos).unwrapKey()
                    .map(key -> translateBiome(key.location().getNamespace(), key.location().getPath()))
                    .orElse(unknown);
        } catch (Exception e) { return Lang.isRussian() ? "Неизвестно" : "Unknown"; }
    }

    private String translateBiome(String namespace, String raw) {
        // If English — return nicely formatted raw name without Russian lookup
        if (!Lang.isRussian()) {
            // Format: "cave/andesite_caves" -> "Andesite Caves", "alpha_islands" -> "Alpha Islands"
            String stripped = raw.contains("/") ? raw.substring(raw.lastIndexOf('/') + 1) : raw;
            String[] words = stripped.split("_");
            StringBuilder sb = new StringBuilder();
            for (String w : words) {
                if (sb.length() > 0) sb.append(' ');
                if (w.length() > 0) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
            }
            return sb.toString();
        }
        // Сначала проверяем Terralith
        if ("terralith".equals(namespace)) {
            Map<String, String> t = new LinkedHashMap<>();
            t.put("alpha_islands","Альфа острова"); t.put("alpha_islands_winter","Альфа острова (Зима)");
            t.put("alpine_grove","Альпийская роща"); t.put("alpine_highlands","Альпийское нагорье");
            t.put("amethyst_canyon","Аметистовый каньон"); t.put("amethyst_rainforest","Аметистовый тропический лес");
            t.put("ancient_sands","Древние пески"); t.put("arid_highlands","Засушливое нагорье");
            t.put("ashen_savanna","Пепельная саванна"); t.put("basalt_cliffs","Базальтовые скалы");
            t.put("birch_taiga","Берёзовая тайга"); t.put("blooming_plateau","Цветущее плато");
            t.put("blooming_valley","Цветущая долина"); t.put("brushland","Кустарниковые земли");
            t.put("bryce_canyon","Каньон Брайс"); t.put("caldera","Кальдера");
            t.put("cloud_forest","Облачный лес"); t.put("cold_shrubland","Холодные кустарники");
            t.put("deep_warm_ocean","Глубокий тёплый океан"); t.put("desert_canyon","Пустынный каньон");
            t.put("desert_oasis","Пустынный оазис"); t.put("desert_spires","Пустынные шпили");
            t.put("emerald_peaks","Изумрудные вершины"); t.put("forested_highlands","Лесное нагорье");
            t.put("fractured_savanna","Расколотая саванна"); t.put("frozen_cliffs","Замёрзшие скалы");
            t.put("glacial_chasm","Ледниковая расщелина"); t.put("granite_cliffs","Гранитные скалы");
            t.put("gravel_beach","Галечный пляж"); t.put("gravel_desert","Гравийная пустыня");
            t.put("haze_mountain","Туманная гора"); t.put("highlands","Нагорье");
            t.put("hot_shrubland","Жаркие кустарники"); t.put("ice_marsh","Ледяное болото");
            t.put("jungle_mountains","Горы джунглей"); t.put("lavender_forest","Лавандовый лес");
            t.put("lavender_valley","Лавандовая долина"); t.put("lush_desert","Пышная пустыня");
            t.put("lush_valley","Пышная долина"); t.put("mirage_isles","Острова-мираж");
            t.put("moonlight_grove","Лунная роща"); t.put("moonlight_valley","Лунная долина");
            t.put("orchid_swamp","Орхидейное болото"); t.put("painted_mountains","Расписные горы");
            t.put("red_oasis","Красный оазис"); t.put("rocky_jungle","Каменистые джунгли");
            t.put("rocky_mountains","Скалистые горы"); t.put("rocky_shrubland","Каменистые кустарники");
            t.put("sakura_grove","Сакуровая роща"); t.put("sakura_valley","Долина сакуры");
            t.put("savanna_slopes","Склоны саванны"); t.put("scarlet_mountains","Алые горы");
            t.put("scorched_desert","Выжженная пустыня"); t.put("scorched_wetlands","Выжженные болота");
            t.put("shrubland","Кустарники"); t.put("skylands_autumn","Небесные острова (Осень)");
            t.put("skylands_spring","Небесные острова (Весна)"); t.put("skylands_summer","Небесные острова (Лето)");
            t.put("skylands_winter","Небесные острова (Зима)"); t.put("snowy_badlands","Снежные бедленды");
            t.put("snowy_cherry_grove","Снежная вишнёвая роща"); t.put("snowy_maple_forest","Снежный кленовый лес");
            t.put("snowy_oak_forest","Снежный дубовый лес"); t.put("snowy_shield","Снежный щит");
            t.put("snowy_slopes","Снежные склоны"); t.put("sparse_jungle","Редкие джунгли");
            t.put("stony_spires","Каменистые шпили"); t.put("temperate_highlands","Умеренное нагорье");
            t.put("tropical_jungle","Тропические джунгли"); t.put("tundra","Тундра");
            t.put("valley_clearing","Долинная поляна"); t.put("volcanic_crater","Вулканический кратер");
            t.put("volcanic_peaks","Вулканические вершины"); t.put("white_cliffs","Белые скалы");
            t.put("yellowstone","Йеллоустоун"); t.put("yosemite_cliffs","Скалы Йосемити");
            t.put("yosemite_lowlands","Низины Йосемити");
            // Пещеры
            t.put("cave/andesite_caves","Андезитовые пещеры"); t.put("cave/crystal_caves","Кристальные пещеры");
            t.put("cave/diorite_caves","Диоритовые пещеры"); t.put("cave/frostfire_caves","Морозно-огненные пещеры");
            t.put("cave/fungal_caves","Грибные пещеры"); t.put("cave/granite_caves","Гранитные пещеры");
            t.put("cave/infested_caves","Заражённые пещеры"); t.put("cave/mantle_caves","Мантийные пещеры");
            t.put("cave/marble_caves","Мраморные пещеры"); t.put("cave/microbial_caves","Микробные пещеры");
            t.put("cave/thermal_caves","Термальные пещеры"); t.put("cave/underground_jungle","Подземные джунгли");
            if (t.containsKey(raw)) return t.get(raw);
            // Если не нашли — красиво форматируем
            return raw.replace("cave/","").replace("_"," ");
        }
        return translateBiome(raw);
    }

    private String translateBiome(String raw) {
        if (!Lang.isRussian()) {
            String[] words = raw.split("_");
            StringBuilder sb = new StringBuilder();
            for (String w : words) {
                if (sb.length() > 0) sb.append(' ');
                if (w.length() > 0) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
            }
            return sb.toString();
        }
        Map<String, String> b = new LinkedHashMap<>();
        b.put("plains","Равнина"); b.put("sunflower_plains","Подсолнечная равнина");
        b.put("forest","Лес"); b.put("flower_forest","Цветочный лес");
        b.put("birch_forest","Берёзовый лес"); b.put("old_growth_birch_forest","Старый берёзовый лес");
        b.put("dark_forest","Тёмный лес"); b.put("jungle","Джунгли");
        b.put("sparse_jungle","Редкие джунгли"); b.put("bamboo_jungle","Бамбуковые джунгли");
        b.put("taiga","Тайга"); b.put("snowy_taiga","Снежная тайга");
        b.put("old_growth_pine_taiga","Старая сосновая тайга"); b.put("old_growth_spruce_taiga","Старая еловая тайга");
        b.put("desert","Пустыня"); b.put("savanna","Саванна");
        b.put("savanna_plateau","Плато саванны"); b.put("windswept_savanna","Продуваемая саванна");
        b.put("badlands","Бедленды"); b.put("wooded_badlands","Лесистые бедленды");
        b.put("eroded_badlands","Размытые бедленды"); b.put("swamp","Болото");
        b.put("mangrove_swamp","Мангровое болото"); b.put("beach","Пляж");
        b.put("snowy_beach","Снежный пляж"); b.put("stony_shore","Каменистый берег");
        b.put("river","Река"); b.put("frozen_river","Замёрзшая река");
        b.put("ocean","Океан"); b.put("deep_ocean","Глубокий океан");
        b.put("cold_ocean","Холодный океан"); b.put("deep_cold_ocean","Глубокий холодный океан");
        b.put("lukewarm_ocean","Тёплый океан"); b.put("deep_lukewarm_ocean","Глубокий тёплый океан");
        b.put("warm_ocean","Тропический океан"); b.put("frozen_ocean","Замёрзший океан");
        b.put("deep_frozen_ocean","Глубокий замёрзший океан"); b.put("mushroom_fields","Грибные поля");
        b.put("ice_spikes","Ледяные шипы"); b.put("snowy_plains","Снежная равнина");
        b.put("snowy_slopes","Снежные склоны"); b.put("grove","Роща");
        b.put("meadow","Луг"); b.put("cherry_grove","Вишнёвая роща");
        b.put("jagged_peaks","Зазубренные пики"); b.put("frozen_peaks","Замёрзшие пики");
        b.put("stony_peaks","Каменные пики"); b.put("windswept_hills","Продуваемые холмы");
        b.put("windswept_gravelly_hills","Продуваемые галечные холмы"); b.put("windswept_forest","Продуваемый лес");
        b.put("nether_wastes","Пустоши Незера"); b.put("soul_sand_valley","Долина душ");
        b.put("crimson_forest","Багровый лес"); b.put("warped_forest","Искажённый лес");
        b.put("basalt_deltas","Базальтовые дельты"); b.put("the_end","Край");
        b.put("end_highlands","Возвышенности Края"); b.put("end_midlands","Срединные земли Края");
        b.put("end_barrens","Пустоши Края"); b.put("small_end_islands","Малые острова Края");
        b.put("the_void","Пустота"); b.put("deep_dark","Глубокая тьма");
        b.put("dripstone_caves","Капельниковые пещеры"); b.put("lush_caves","Пышные пещеры");
        return b.getOrDefault(raw, raw);
    }

    private void renderDurability(GuiGraphics graphics, Player player, Minecraft mc) {
        // Собираем пары (стак, прочность)
        java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>();

        net.minecraft.world.entity.EquipmentSlot[] slots = {
            net.minecraft.world.entity.EquipmentSlot.HEAD,
            net.minecraft.world.entity.EquipmentSlot.CHEST,
            net.minecraft.world.entity.EquipmentSlot.LEGS,
            net.minecraft.world.entity.EquipmentSlot.FEET
        };
        for (net.minecraft.world.entity.EquipmentSlot slot : slots) {
            net.minecraft.world.item.ItemStack s = player.getItemBySlot(slot);
            if (!s.isEmpty() && s.isDamageableItem()) items.add(s);
        }
        net.minecraft.world.item.ItemStack main = player.getMainHandItem();
        if (!main.isEmpty() && main.isDamageableItem()) items.add(main);
        net.minecraft.world.item.ItemStack off = player.getOffhandItem();
        if (!off.isEmpty() && off.isDamageableItem()) items.add(off);

        if (items.isEmpty()) return;

        // Константы — иконка 16px, отступ 2px, текст справа
        final int ICON  = 16;
        final int GAP   = 3;
        final int ROW_H = 18; // высота строки
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // Считаем максимальную ширину текста
        int maxTextW = 0;
        for (net.minecraft.world.item.ItemStack s : items) {
            int cur = s.getMaxDamage() - s.getDamageValue();
            int textW = mc.font.width(String.valueOf(cur));
            if (textW > maxTextW) maxTextW = textW;
        }

        int totalW = ICON + GAP + maxTextW + GAP * 2;
        int totalH = ROW_H * items.size() + GAP;

        com.infopanel.client.PanelEditScreen.durabilityW = totalW;
        com.infopanel.client.PanelEditScreen.durabilityH = totalH;

        int x, y;
        if (InfoPanelConfig.getDurabilityX() >= 0 && InfoPanelConfig.getDurabilityY() >= 0) {
            x = InfoPanelConfig.getDurabilityX();
            y = InfoPanelConfig.getDurabilityY();
        } else {
            // Дефолт: правый нижний угол над хотбаром
            x = sw - totalW - 5;
            y = sh - 55 - totalH;
        }

        // Прозрачный фон
        int bgAlpha = InfoPanelConfig.getBgAlpha();
        if (bgAlpha > 0)
            graphics.fill(x - 2, y - 2, x + totalW + 2, y + totalH + 2,
                    (bgAlpha << 24) | 0x000000);

        int iy = y + GAP;
        for (net.minecraft.world.item.ItemStack s : items) {
            int cur = s.getMaxDamage() - s.getDamageValue();
            int max = s.getMaxDamage();
            int color = getDurColor(cur, max);

            // Иконка предмета
            graphics.renderItem(s, x + GAP, iy);

            // Число прочности справа от иконки, по центру по вертикали
            String text = String.valueOf(cur);
            int textX = x + GAP + ICON + GAP;
            int textY = iy + (ICON - mc.font.lineHeight) / 2;
            graphics.drawString(mc.font,
                net.minecraft.network.chat.Component.literal(text),
                textX, textY, color, true);

            iy += ROW_H;
        }
    }



    private int getDurColor(int cur, int max) {
        float pct = (float) cur / max;
        if (pct <= 0.1f && (System.currentTimeMillis() / 500) % 2 == 0) return 0xFF0000;
        if (pct <= 0.25f) return InfoPanelConfig.colorDurabilityWarn;
        return InfoPanelConfig.colorDurability;
    }

}