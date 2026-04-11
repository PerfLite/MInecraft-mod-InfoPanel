package com.infopanel.client;

import com.infopanel.config.InfoPanelConfig;
import com.infopanel.event.CompassRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Экран редактирования позиции HUD-элементов.
 * Поддерживает перетаскивание панели и компаса независимо.
 */
public class PanelEditScreen extends Screen {

    // Bounds панели — передаются из HudRenderHandler
    public static int panelW = 100;
    public static int panelH = 80;

    // Bounds компаса — передаются из CompassRenderer
    public static int compassW = 130;
    public static int compassH = 22;

    // Bounds прочности — передаются из HudRenderHandler
    public static int durabilityW = 100;
    public static int durabilityH = 10;

    // Что сейчас тащим
    private enum DragTarget { NONE, PANEL, COMPASS, DURABILITY }
    private DragTarget dragging = DragTarget.NONE;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    // Пульсация рамок
    private float pulsePanelF  = 0f;
    private float pulseCompassF = 0.5f; // чуть сдвинуто для разнообразия
    private int pulsePanelDir  = 1;
    private int pulseCompassDir = 1;

    public PanelEditScreen() {
        super(Component.literal(""));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ─── Позиции элементов ───────────────────────────────────────────────

    private int getPanelX() {
        if (InfoPanelConfig.panelX >= 0) return InfoPanelConfig.panelX;
        switch (InfoPanelConfig.getPosition()) {
            case TOP_RIGHT:    return this.width - panelW - 6;
            case BOTTOM_LEFT:  return 0;
            case BOTTOM_RIGHT: return this.width - panelW - 6;
            default:           return 0;
        }
    }

    private int getPanelY() {
        if (InfoPanelConfig.panelY >= 0) return InfoPanelConfig.panelY;
        switch (InfoPanelConfig.getPosition()) {
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT: return this.height - panelH - 6;
            default:           return 0;
        }
    }

    private int getCompassX() {
        if (InfoPanelConfig.compassX >= 0) return InfoPanelConfig.compassX;
        return (this.width - compassW) / 2;
    }

    private int getCompassY() {
        if (InfoPanelConfig.compassY >= 0) return InfoPanelConfig.compassY;
        return 2;
    }

    private int getDurabilityX() {
        if (InfoPanelConfig.durabilityX >= 0) return InfoPanelConfig.durabilityX;
        return this.width / 2 - durabilityW / 2;
    }

    private int getDurabilityY() {
        if (InfoPanelConfig.durabilityY >= 0) return InfoPanelConfig.durabilityY;
        return this.height - 60;
    }

    // ─── Ввод ────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        // Сначала проверяем компас (он обычно меньше, приоритет выше)
        if (InfoPanelConfig.isShowCompassBar()) {
            int cx = getCompassX(), cy = getCompassY();
            if (mx >= cx && mx <= cx + compassW && my >= cy && my <= cy + compassH) {
                dragging = DragTarget.COMPASS;
                dragOffsetX = (int)(mx - cx);
                dragOffsetY = (int)(my - cy);
                return true;
            }
        }

        if (InfoPanelConfig.isShowDurability()) {
            int dx2 = getDurabilityX(), dy2 = getDurabilityY();
            if (mx >= dx2 && mx <= dx2 + durabilityW && my >= dy2 && my <= dy2 + durabilityH) {
                dragging = DragTarget.DURABILITY;
                dragOffsetX = (int)(mx - dx2);
                dragOffsetY = (int)(my - dy2);
                return true;
            }
        }

        int px = getPanelX(), py = getPanelY();
        if (mx >= px && mx <= px + panelW && my >= py && my <= py + panelH) {
            dragging = DragTarget.PANEL;
            dragOffsetX = (int)(mx - px);
            dragOffsetY = (int)(my - py);
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && dragging != DragTarget.NONE) {
            dragging = DragTarget.NONE;
            InfoPanelConfig.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button != 0) return super.mouseDragged(mx, my, button, dx, dy);

        if (dragging == DragTarget.PANEL) {
            int nx = Math.max(0, Math.min((int)(mx - dragOffsetX), this.width  - panelW));
            int ny = Math.max(0, Math.min((int)(my - dragOffsetY), this.height - panelH));
            InfoPanelConfig.panelX = nx;
            InfoPanelConfig.panelY = ny;
            return true;
        }

        if (dragging == DragTarget.COMPASS) {
            int nx = Math.max(0, Math.min((int)(mx - dragOffsetX), this.width  - compassW));
            int ny = Math.max(0, Math.min((int)(my - dragOffsetY), this.height - compassH));
            InfoPanelConfig.compassX = nx;
            InfoPanelConfig.compassY = ny;
            return true;
        }

        if (dragging == DragTarget.DURABILITY) {
            int nx = Math.max(0, Math.min((int)(mx - dragOffsetX), this.width  - durabilityW));
            int ny = Math.max(0, Math.min((int)(my - dragOffsetY), this.height - durabilityH));
            InfoPanelConfig.durabilityX = nx;
            InfoPanelConfig.durabilityY = ny;
            return true;
        }

        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Esc
            InfoPanelConfig.save();
            this.minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ─── Рендер ──────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Обновляем пульсацию
        pulsePanelF  += 0.04f * pulsePanelDir;
        pulseCompassF += 0.04f * pulseCompassDir;
        if (pulsePanelF  > 1f) { pulsePanelF  = 1f; pulsePanelDir  = -1; }
        if (pulsePanelF  < 0f) { pulsePanelF  = 0f; pulsePanelDir  =  1; }
        if (pulseCompassF > 1f) { pulseCompassF = 1f; pulseCompassDir = -1; }
        if (pulseCompassF < 0f) { pulseCompassF = 0f; pulseCompassDir =  1; }

        // Полупрозрачный оверлей
        graphics.fill(0, 0, this.width, this.height, 0x55000000);

        // Рисуем рамку панели
        drawElementFrame(graphics, getPanelX(), getPanelY(), panelW, panelH,
                pulsePanelF, dragging == DragTarget.PANEL, 0x00AAFF);

        // Рисуем рамку компаса (только если включён)
        if (InfoPanelConfig.isShowCompassBar()) {
            drawElementFrame(graphics, getCompassX(), getCompassY(), compassW, compassH,
                    pulseCompassF, dragging == DragTarget.COMPASS, 0xFFAA00);
        }

        // Рисуем рамку прочности (только если включена)
        if (InfoPanelConfig.isShowDurability()) {
            drawElementFrame(graphics, getDurabilityX(), getDurabilityY(), durabilityW, durabilityH,
                    pulsePanelF, dragging == DragTarget.DURABILITY, 0x55FF55);
        }

        // Подсказка сверху
        String hint;
        if (dragging == DragTarget.PANEL) {
            hint = "§bПанель§r  X: " + InfoPanelConfig.panelX + "  Y: " + InfoPanelConfig.panelY;
        } else if (dragging == DragTarget.COMPASS) {
            hint = "§eКомпас§r  X: " + InfoPanelConfig.compassX + "  Y: " + InfoPanelConfig.compassY;
        } else if (dragging == DragTarget.DURABILITY) {
            hint = "§aПрочность§r  X: " + InfoPanelConfig.durabilityX + "  Y: " + InfoPanelConfig.durabilityY;
        } else {
            hint = "§eРежим редактирования  §7— Тащи панель §b(синяя)§7, компас §e(жёлтая)§7 или прочность §a(зелёная)§7  |  Esc";
        }
        int tw = this.minecraft.font.width(hint.replaceAll("§.", ""));
        graphics.drawString(this.minecraft.font, hint, (this.width - tw) / 2, 6, 0xFFFFFF, true);

        // Метки под элементами
        drawLabel(graphics, "Панель",  getPanelX(),   getPanelY()   + panelH  + 4, panelW,  0xAABBFF);
        if (InfoPanelConfig.isShowCompassBar())
            drawLabel(graphics, "Компас", getCompassX(), getCompassY() + compassH + 4, compassW, 0xFFDD88);
        if (InfoPanelConfig.isShowDurability())
            drawLabel(graphics, "Прочность", getDurabilityX(), getDurabilityY() + durabilityH + 4, durabilityW, 0x88FF88);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawElementFrame(GuiGraphics g, int x, int y, int w, int h,
                                   float pulse, boolean active, int rgb) {
        // Подсветка области
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, active ? 0x44FFFFFF : 0x22FFFFFF);

        // Пульсирующая рамка
        int alpha = active ? 220 : (int)(60 + pulse * 160);
        int col   = (alpha << 24) | rgb;

        g.fill(x - 2, y - 2,     x + w + 2, y - 1,     col); // верх
        g.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, col); // низ
        g.fill(x - 2, y - 2,     x - 1,     y + h + 2, col); // лево
        g.fill(x + w + 1, y - 2, x + w + 2, y + h + 2, col); // право
    }

    private void drawLabel(GuiGraphics g, String text, int elX, int labelY, int elW, int color) {
        int tw = this.minecraft.font.width(text);
        int lx = elX + (elW - tw) / 2;
        g.drawString(this.minecraft.font, text, lx, labelY, color, true);
    }
}
