package com.infopanel.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class ColorButton extends Button {

    private final IntSupplier colorGetter;

    public ColorButton(int x, int y, int w, int h, IntSupplier colorGetter, OnPress onPress) {
        super(x, y, w, h, Component.empty(), onPress, DEFAULT_NARRATION);
        this.colorGetter = colorGetter;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Рамка кнопки
        int border = isHovered() ? 0xFFFFFFFF : 0xFF888888;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
        // Заливка цветом без текста
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1,
                0xFF000000 | colorGetter.getAsInt());
    }
}
