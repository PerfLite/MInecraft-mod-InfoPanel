package com.infopanel.event;

import com.infopanel.client.PanelEditScreen;
import com.infopanel.config.InfoPanelConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public class CompassRenderer {

    // Размеры компаса (в «сырых» пикселях до scale)
    public static final int COMPASS_W = 120;
    public static final int COMPASS_H = 16;
    // Паддинг фона
    private static final int PAD = 3;

    @SubscribeEvent
    public void onRenderHud(RenderGuiEvent.Post event) {
        if (!InfoPanelConfig.hudVisible) return;
        if (!InfoPanelConfig.isShowCompassBar()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)
                && !(mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;
        if (mc.options.hideGui) return;
        if (mc.player.isSpectator()) return;

        Player player = mc.player;
        GuiGraphics graphics = event.getGuiGraphics();
        float yRot = player.getYRot();
        float scale = InfoPanelConfig.getScale();

        int screenW = mc.getWindow().getGuiScaledWidth();

        // Полные размеры с паддингом и scale
        int totalW = (int)((COMPASS_W + PAD * 2) * scale);
        int totalH = (int)((COMPASS_H + PAD * 2) * scale);

        int screenH = mc.getWindow().getGuiScaledHeight();

        // Позиция: если задана вручную — используем, иначе дефолт (центр сверху)
        // Если сохранённая позиция выходит за экран (например сменился размер окна) — сбрасываем
        int x, y;
        int savedX = InfoPanelConfig.getCompassX();
        int savedY = InfoPanelConfig.getCompassY();
        if (savedX >= 0 && savedY >= 0 && savedX < screenW && savedY < screenH) {
            x = savedX;
            y = savedY;
        } else {
            x = (screenW - totalW) / 2;
            y = 2;
        }

        // Сообщаем PanelEditScreen наши bounds
        PanelEditScreen.compassW = totalW;
        PanelEditScreen.compassH = totalH;

        // Фона нет — компас всегда прозрачный
        graphics.pose().pushPose();
        graphics.pose().translate(x + PAD * scale, y + PAD * scale, 0);
        graphics.pose().scale(scale, scale, 1f);

        renderCompassBar(graphics, mc, yRot, 0, 0);

        graphics.pose().popPose();
    }

    public static void renderCompassBar(GuiGraphics g, Minecraft mc, float yRot, int x, int y) {
        // yRot → градусы: MC 0=Юг → наш 180=Юг, так что deg=0 это Север
        float deg = ((yRot + 180f) % 360f + 360f) % 360f;

        boolean ru = Lang.isRussian();
        record Mark(float angle, String label, boolean cardinal) {}
        Mark[] marks = {
            new Mark(0f,   ru ? "С"  : "N",  true),
            new Mark(45f,  ru ? "СВ" : "NE", false),
            new Mark(90f,  ru ? "В"  : "E",  true),
            new Mark(135f, ru ? "ЮВ" : "SE", false),
            new Mark(180f, ru ? "Ю"  : "S",  true),
            new Mark(225f, ru ? "ЮЗ" : "SW", false),
            new Mark(270f, ru ? "З"  : "W",  true),
            new Mark(315f, ru ? "СЗ" : "NW", false),
        };

        int halfW    = COMPASS_W / 2;
        float pxPerDeg = COMPASS_W / 90f;
        int barY     = y + 2;

        // Метки
        for (Mark mark : marks) {
            float delta = mark.angle() - deg;
            delta = ((delta + 180f) % 360f + 360f) % 360f - 180f;
            if (Math.abs(delta) > 52f) continue;

            int mx    = x + halfW + (int)(delta * pxPerDeg);
            int textW = mc.font.width(mark.label());

            if (mark.cardinal()) {
                boolean isNorth = mark.angle() == 0f;
                boolean isSouth = mark.angle() == 180f;
                int col = isNorth ? 0xFF55FF55 : isSouth ? 0xFFFF5555 : 0xFF55FFFF;
                drawOutlined(g, mc, mark.label(), mx - textW / 2, barY + 2, col);
            } else {
                drawOutlined(g, mc, mark.label(), mx - textW / 2, barY + 2, 0xFF777777);
            }
        }

        // Указатель ▼ по центру сверху
        int cx = x + halfW;
        g.fill(cx - 1, barY - 2, cx + 2, barY - 1, 0xFFFFFFFF);
        g.fill(cx,     barY - 1, cx + 1, barY,     0xFFFFFFFF);
    }

    private static void drawOutlined(GuiGraphics g, Minecraft mc, String text, int x, int y, int color) {
        g.drawString(mc.font, text, x, y, color, true);
    }
}
