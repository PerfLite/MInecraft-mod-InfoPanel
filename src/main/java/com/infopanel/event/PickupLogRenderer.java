package com.infopanel.event;

import com.infopanel.config.InfoPanelConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@EventBusSubscriber(modid = "infopanel", value = Dist.CLIENT)
public class PickupLogRenderer {

    // Время анимации исчезновения (последние 20 тиков = 1 сек)
    private static final int FADE_TICKS = 20;

    private static final int BASE_ICON = 16;  // базовый размер иконки
    private static final int BASE_PAD  = 4;   // базовый паддинг
    private static final int BASE_ROW  = 18;  // базовая высота строки

    /** Одна запись в логе */
    private record PickupEntry(ItemStack stack, long addedTick) {
        int lifetimeTicks() {
            return InfoPanelConfig.getPickupLogLifetime() * 20;
        }
        long remaining(long currentTick) {
            return lifetimeTicks() - (currentTick - addedTick);
        }
        float alpha(long currentTick) {
            long rem = remaining(currentTick);
            if (rem <= 0) return 0f;
            if (rem >= FADE_TICKS) return 1f;
            return rem / (float) FADE_TICKS;
        }
    }

    private static final List<PickupEntry> entries = new ArrayList<>();
    private static final Deque<ItemStack> pendingPickups = new ArrayDeque<>();
    private static long currentTick = 0;

    // Публичные bounds для PanelEditScreen
    public static int pickupLogW = 160;
    public static int pickupLogH = BASE_ROW;

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!event.getPlayer().getUUID().equals(mc.player.getUUID())) return;
        if (!InfoPanelConfig.isShowPickupLog()) return;

        ItemStack picked = event.getOriginalStack();
        if (picked.isEmpty()) return;

        synchronized (pendingPickups) {
            pendingPickups.addLast(picked.copy());
        }
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        if (!InfoPanelConfig.hudVisible) return;
        if (!InfoPanelConfig.isShowPickupLog()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return;
        if (mc.screen != null
                && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)
                && !(mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;

        currentTick++;

        synchronized (pendingPickups) {
            while (!pendingPickups.isEmpty()) {
                addEntry(pendingPickups.pollFirst());
            }
        }

        entries.removeIf(e -> e.remaining(currentTick) <= 0);
        if (entries.isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        float scale = InfoPanelConfig.getPickupLogScale();
        int icon = (int)(BASE_ICON * scale);
        int pad  = (int)(BASE_PAD  * scale);
        int rowH = (int)(BASE_ROW  * scale);
        int fontSize = (int)(mc.font.lineHeight * scale);

        // Вычисляем ширину по самому длинному тексту
        int maxTextW = 0;
        for (PickupEntry e : entries) {
            String text = buildText(e);
            int tw = (int)(mc.font.width(text) * scale);
            if (tw > maxTextW) maxTextW = tw;
        }

        int totalW = pad + maxTextW + pad + icon + pad + 8; // текст слева, иконка справа
        int totalH = rowH * entries.size() + pad;

        pickupLogW = totalW;
        pickupLogH = totalH;

        int x, y;
        int savedX = InfoPanelConfig.getPickupLogX();
        int savedY = InfoPanelConfig.getPickupLogY();
        if (savedX >= 0 && savedY >= 0 && savedX < screenW && savedY < screenH) {
            x = savedX;
            y = savedY;
        } else {
            x = screenW - totalW - 5;
            y = screenH - 55 - totalH;
        }

        int bgAlpha = InfoPanelConfig.getPickupLogBgAlpha();

        // Рисуем каждую строку
        for (int i = 0; i < entries.size(); i++) {
            PickupEntry entry = entries.get(i);
            float alpha = entry.alpha(currentTick);
            int rowY = y + i * rowH;

            // Кастомный фон: острый левый угол, закруглённый правый
            if (bgAlpha > 0) {
                int a = (int)(bgAlpha * alpha);
                drawPickupBackground(graphics, x, rowY, totalW, rowH, a);
            }

            // Текст слева (количество + название)
            String text = buildText(entry);
            int textColor = ((int)(alpha * 255) << 24) | 0xFFFFFF;
            int textX = x + pad;
            int textY = rowY + (rowH - fontSize) / 2;

            graphics.pose().pushPose();
            graphics.pose().translate(textX, textY, 0);
            graphics.pose().scale(scale, scale, 1f);
            graphics.drawString(mc.font, text, 0, 0, textColor, true);
            graphics.pose().popPose();

            // Иконка справа от текста
            graphics.pose().pushPose();
            graphics.pose().translate(x + pad + maxTextW + pad, rowY + (rowH - icon) / 2f, 0);
            graphics.pose().scale(scale, scale, 1f);
            graphics.renderItem(entry.stack(), 0, 0);
            graphics.pose().popPose();
        }
    }

    /**
     * Рисует фон строки:
     * - Левый угол: острый (треугольный срез)
     * - Правый угол: закруглённый (ступенчатое приближение)
     */
    private static void drawPickupBackground(GuiGraphics g, int x, int y, int w, int h, int alpha) {
        int col = (alpha << 24) | 0x111111;
        int r = Math.min(h / 2, 6); // радиус закругления справа

        // Основной прямоугольник (без углов)
        g.fill(x + r, y, x + w - r, y + h, col);

        // Левый острый угол — треугольник (рисуем горизонтальными полосками)
        for (int dy = 0; dy < h; dy++) {
            // Чем ближе к середине — тем шире
            float t = 1f - Math.abs(dy - h / 2f) / (h / 2f);
            int indent = (int)(r * (1f - t));
            g.fill(x + indent, y + dy, x + r, y + dy + 1, col);
        }

        // Правый закруглённый угол — ступенчатое приближение окружности
        for (int dy = 0; dy < h; dy++) {
            float t = Math.abs(dy - (h - 1) / 2f) / (h / 2f); // 0 в центре, 1 на краях
            int cut = (int)(r * t * t); // квадратичное закругление
            g.fill(x + w - r, y + dy, x + w - cut, y + dy + 1, col);
        }
    }

    private static String buildText(PickupEntry entry) {
        int count = entry.stack().getCount();
        return "§a+" + count;
    }

    private static void addEntry(ItemStack stack) {
        int maxItems = InfoPanelConfig.getPickupLogMaxItems();

        // Объединяем одинаковые предметы
        for (int i = 0; i < entries.size(); i++) {
            PickupEntry existing = entries.get(i);
            if (ItemStack.isSameItemSameComponents(existing.stack(), stack)) {
                ItemStack merged = existing.stack().copy();
                merged.setCount(existing.stack().getCount() + stack.getCount());
                entries.set(i, new PickupEntry(merged, currentTick));
                return;
            }
        }

        entries.add(new PickupEntry(stack.copy(), currentTick));

        while (entries.size() > maxItems) {
            entries.remove(0);
        }
    }
}
