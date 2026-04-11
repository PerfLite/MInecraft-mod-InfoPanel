package com.infopanel.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Matrix4f;

public class EffectTimerRenderer {

    public static void renderSingleTimer(GuiGraphics graphics, Minecraft mc,
                                         MobEffectInstance effect, int ex, int ey, int iconSize) {
        String time = formatDuration(effect.getDuration());

        float scale = 0.75f;
        int scaledW = (int)(mc.font.width(time) * scale);
        int tx = ex + (iconSize - scaledW) / 2;
        int ty = ey + iconSize - (int)(mc.font.lineHeight * scale) - 1;

        graphics.pose().pushPose();
        graphics.pose().translate(tx, ty, 0);
        graphics.pose().scale(scale, scale, 1f);

        graphics.drawString(mc.font, time, -1,  0, 0xFF000000, false);
        graphics.drawString(mc.font, time,  1,  0, 0xFF000000, false);
        graphics.drawString(mc.font, time,  0, -1, 0xFF000000, false);
        graphics.drawString(mc.font, time,  0,  1, 0xFF000000, false);
        graphics.drawString(mc.font, time,  0,  0, 0xFFFFFFFF, false);

        graphics.pose().popPose();
    }

    private static String formatDuration(int ticks) {
        if (ticks >= 32767 * 20 || ticks == Integer.MAX_VALUE) return "∞";
        int seconds = ticks / 20;
        if (seconds < 60) return seconds + "s";
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}
