package com.infopanel.client;

import com.infopanel.config.InfoPanelConfig;
import com.infopanel.config.InfoPanelConfig.Position;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class InfoPanelConfigScreen extends Screen {

    private final Screen parent;

    // Текущая вкладка: 0 = Панель, 1 = Строки, 2 = WAILA & Прочее
    private int activeTab = 0;
    private static final String[] TAB_LABELS = { "  Панель  ", "  Строки  ", "  WAILA  ", "  Структуры  " };

    private static final int[] COLORS = {
        0xFFFFFF, 0xAAAAAA, 0x555555,
        0xFF5555, 0xFF9900, 0xFFFF55,
        0x55FF55, 0x00AA00,
        0x55FFFF, 0x0099FF, 0x0000AA,
        0xFF55FF, 0xAA00AA,
    };

    // Константы layout
    private static final int TAB_H    = 20;
    private static final int TAB_Y    = 14;
    private static final int CONTENT_Y = TAB_Y + TAB_H + 6;
    private static final int BTN_H    = 20;
    private static final int STEP     = 23;

    public InfoPanelConfigScreen(Screen parent) {
        super(Component.literal("InfoPanel — Настройки"));
        this.parent = parent;
        InfoPanelConfig.load();
    }

    @Override
    protected void init() {
        int cx   = this.width / 2;
        int tabW = 80;
        int totalW = TAB_LABELS.length * (tabW + 2);
        int tabStartX = cx - totalW / 2;

        // ── Кнопки вкладок ──────────────────────────────────────────────
        for (int i = 0; i < TAB_LABELS.length; i++) {
            final int idx = i;
            String label = (activeTab == idx ? "§e" : "§7") + TAB_LABELS[idx];
            this.addRenderableWidget(
                Button.builder(Component.literal(label), btn -> {
                    activeTab = idx;
                    rebuildWidgets();
                }).bounds(tabStartX + idx * (tabW + 2), TAB_Y, tabW, TAB_H).build()
            );
        }

        // ── Контент вкладки ──────────────────────────────────────────────
        switch (activeTab) {
            case 0 -> buildTabPanel(cx);
            case 1 -> buildTabRows(cx);
            case 2 -> buildTabWaila(cx);
            case 3 -> buildTabStructures(cx);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // Вкладка 0 — Панель (позиция, размер, фон)
    // ════════════════════════════════════════════════════════════════════
    private void buildTabPanel(int cx) {
        int w = 220;
        int y = CONTENT_Y;

        // Слайдер прозрачности фона
        this.addRenderableWidget(new AbstractSliderButton(cx - w / 2, y, w, BTN_H,
                Component.literal("Фон панели: " + InfoPanelConfig.getBgAlpha()),
                InfoPanelConfig.getBgAlpha() / 255.0) {
            @Override protected void updateMessage() {
                InfoPanelConfig.setBgAlpha((int)(value * 255));
                setMessage(Component.literal("Фон панели: " + InfoPanelConfig.getBgAlpha()));
            }
            @Override protected void applyValue() { InfoPanelConfig.setBgAlpha((int)(value * 255)); }
        });
        y += STEP;

        // Слайдер масштаба
        this.addRenderableWidget(new AbstractSliderButton(cx - w / 2, y, w, BTN_H,
                Component.literal(String.format("Масштаб: %.1f×", InfoPanelConfig.getScale())),
                (InfoPanelConfig.getScale() - 0.5) / 1.5) {
            @Override protected void updateMessage() {
                float s = (float)(0.5 + value * 1.5);
                InfoPanelConfig.setScale(s);
                setMessage(Component.literal(String.format("Масштаб: %.1f×", s)));
            }
            @Override protected void applyValue() { InfoPanelConfig.setScale((float)(0.5 + value * 1.5)); }
        });
        y += STEP + 6;

        // Заголовок «Позиция»
        final int labelY = y;
        // 4 кнопки позиции 2×2
        int hw = w / 2 - 2;
        this.addRenderableWidget(Button.builder(
                Component.literal(pos(Position.TOP_LEFT) + "↖ Верх-Лево"),
                btn -> { InfoPanelConfig.setPosition(Position.TOP_LEFT); rebuildWidgets(); })
                .bounds(cx - w / 2, y, hw, BTN_H).build());
        this.addRenderableWidget(Button.builder(
                Component.literal(pos(Position.TOP_RIGHT) + "Верх-Право ↗"),
                btn -> { InfoPanelConfig.setPosition(Position.TOP_RIGHT); rebuildWidgets(); })
                .bounds(cx + 2, y, hw, BTN_H).build());
        y += STEP;
        this.addRenderableWidget(Button.builder(
                Component.literal(pos(Position.BOTTOM_LEFT) + "↙ Низ-Лево"),
                btn -> { InfoPanelConfig.setPosition(Position.BOTTOM_LEFT); rebuildWidgets(); })
                .bounds(cx - w / 2, y, hw, BTN_H).build());
        this.addRenderableWidget(Button.builder(
                Component.literal(pos(Position.BOTTOM_RIGHT) + "Низ-Право ↘"),
                btn -> { InfoPanelConfig.setPosition(Position.BOTTOM_RIGHT); rebuildWidgets(); })
                .bounds(cx + 2, y, hw, BTN_H).build());
        y += STEP + 6;

        // Редактировать позицию (drag)
        this.addRenderableWidget(
            Button.builder(Component.literal("[ F8 ]  Редактировать позицию мышью"),
                btn -> this.minecraft.setScreen(new PanelEditScreen()))
                .bounds(cx - w / 2, y, w, BTN_H).build());
        y += STEP;

        // Сброс позиции
        this.addRenderableWidget(
            Button.builder(Component.literal("Сброс позиции"),
                btn -> { InfoPanelConfig.panelX = -1; InfoPanelConfig.panelY = -1; InfoPanelConfig.save(); })
                .bounds(cx - 60, y, 120, BTN_H).build());
        y += STEP;

        // Закрыть — прямо под сбросом (только на этой вкладке)
        addCloseButton(cx, y);
    }

    // ════════════════════════════════════════════════════════════════════
    // Вкладка 1 — Строки (тоглы + цвет)
    // ════════════════════════════════════════════════════════════════════
    private void buildTabRows(int cx) {
        int colW = (this.width / 2) - 12;
        int btnW = colW - 28;
        int lx   = cx - colW - 4;
        int rx   = cx + 4;
        int y    = CONTENT_Y;

        // Левая колонка
        addRow(lx, btnW, y + STEP * 0, "Координаты XYZ",
                InfoPanelConfig::isShowCoords, InfoPanelConfig::setShowCoords,
                () -> InfoPanelConfig.colorCoords,
                v -> { InfoPanelConfig.colorCoords = v; InfoPanelConfig.save(); });
        addRow(lx, btnW, y + STEP * 1, "Биом",
                InfoPanelConfig::isShowBiome, InfoPanelConfig::setShowBiome,
                () -> InfoPanelConfig.colorBiome,
                v -> { InfoPanelConfig.colorBiome = v; InfoPanelConfig.save(); });
        addRow(lx, btnW, y + STEP * 2, "FPS",
                InfoPanelConfig::isShowFps, InfoPanelConfig::setShowFps,
                () -> InfoPanelConfig.colorFps,
                v -> { InfoPanelConfig.colorFps = v; InfoPanelConfig.save(); });
        addRow(lx, btnW, y + STEP * 3, "Пинг",
                InfoPanelConfig::isShowPing, InfoPanelConfig::setShowPing,
                () -> InfoPanelConfig.colorPing,
                v -> { InfoPanelConfig.colorPing = v; InfoPanelConfig.save(); });
        addRow(lx, btnW, y + STEP * 4, "Сессия",
                InfoPanelConfig::isShowSession, InfoPanelConfig::setShowSession,
                () -> InfoPanelConfig.colorSession,
                v -> { InfoPanelConfig.colorSession = v; InfoPanelConfig.save(); });

        // Правая колонка
        addRow(rx, btnW, y + STEP * 0, "TPS",
                InfoPanelConfig::isShowTps, InfoPanelConfig::setShowTps,
                () -> InfoPanelConfig.colorTps,
                v -> { InfoPanelConfig.colorTps = v; InfoPanelConfig.save(); });
        addRow(rx, btnW, y + STEP * 1, "Игровое время",
                InfoPanelConfig::isShowTime, InfoPanelConfig::setShowTime,
                () -> InfoPanelConfig.colorTime,
                v -> { InfoPanelConfig.colorTime = v; InfoPanelConfig.save(); });
        addRow(rx, btnW, y + STEP * 2, "Игроки онлайн",
                InfoPanelConfig::isShowPlayers, InfoPanelConfig::setShowPlayers,
                () -> InfoPanelConfig.colorPlayers,
                v -> { InfoPanelConfig.colorPlayers = v; InfoPanelConfig.save(); });
        addToggle(rx, btnW, y + STEP * 3, "Таймеры эффектов",
                InfoPanelConfig::isShowEffectTimers, InfoPanelConfig::setShowEffectTimers);
        addToggle(rx, btnW, y + STEP * 4, "Оверлей света",
                InfoPanelConfig::isShowLightOverlay, InfoPanelConfig::setShowLightOverlay);
        addToggle(rx, btnW, y + STEP * 5, "Слайм чанки",
                InfoPanelConfig::isShowSlimeChunks, InfoPanelConfig::setShowSlimeChunks);
        // Направление (текст в панели) и компас-полоса — отдельно
        addRow(lx, btnW, y + STEP * 5, "Направление (текст)",
                InfoPanelConfig::isShowDirection, InfoPanelConfig::setShowDirection,
                () -> InfoPanelConfig.colorDirection,
                v -> { InfoPanelConfig.colorDirection = v; InfoPanelConfig.save(); });
        addRow(lx, btnW, y + STEP * 6, "Прочность предметов",
                InfoPanelConfig::isShowDurability, InfoPanelConfig::setShowDurability,
                () -> InfoPanelConfig.colorDurability,
                v -> { InfoPanelConfig.colorDurability = v; InfoPanelConfig.save(); });
        addToggle(rx, btnW, y + STEP * 6, "Компас-полоса",
                InfoPanelConfig::isShowCompassBar, InfoPanelConfig::setShowCompassBar);

        addCloseButton(cx, y + STEP * 7 + 4);
    }

    // ════════════════════════════════════════════════════════════════════
    // Вкладка 2 — WAILA & Прочее
    // ════════════════════════════════════════════════════════════════════
    private void buildTabWaila(int cx) {
        int w = 220;
        int y = CONTENT_Y;

        // Фон WAILA
        this.addRenderableWidget(new AbstractSliderButton(cx - w / 2, y, w, BTN_H,
                Component.literal("Фон WAILA: " + InfoPanelConfig.getWailaBgAlpha()),
                InfoPanelConfig.getWailaBgAlpha() / 255.0) {
            @Override protected void updateMessage() {
                InfoPanelConfig.setWailaBgAlpha((int)(value * 255));
                setMessage(Component.literal("Фон WAILA: " + InfoPanelConfig.getWailaBgAlpha()));
            }
            @Override protected void applyValue() { InfoPanelConfig.setWailaBgAlpha((int)(value * 255)); }
        });
        y += STEP + 4;

        // Заголовок позиции WAILA
        int hw = w / 2 - 2;
        this.addRenderableWidget(Button.builder(
                Component.literal(waila(InfoPanelConfig.WailaPosition.TOP_CENTER) + "↑ Верх-Центр"),
                btn -> { InfoPanelConfig.setWailaPosition(InfoPanelConfig.WailaPosition.TOP_CENTER); rebuildWidgets(); })
                .bounds(cx - w / 2, y, hw, BTN_H).build());
        this.addRenderableWidget(Button.builder(
                Component.literal(waila(InfoPanelConfig.WailaPosition.TOP_RIGHT) + "Верх-Право ↗"),
                btn -> { InfoPanelConfig.setWailaPosition(InfoPanelConfig.WailaPosition.TOP_RIGHT); rebuildWidgets(); })
                .bounds(cx + 2, y, hw, BTN_H).build());
        y += STEP;
        this.addRenderableWidget(Button.builder(
                Component.literal(waila(InfoPanelConfig.WailaPosition.BOTTOM_CENTER) + "↓ Низ-Центр"),
                btn -> { InfoPanelConfig.setWailaPosition(InfoPanelConfig.WailaPosition.BOTTOM_CENTER); rebuildWidgets(); })
                .bounds(cx - w / 2, y, hw, BTN_H).build());
        this.addRenderableWidget(Button.builder(
                Component.literal(waila(InfoPanelConfig.WailaPosition.TOP_LEFT) + "↖ Верх-Лево"),
                btn -> { InfoPanelConfig.setWailaPosition(InfoPanelConfig.WailaPosition.TOP_LEFT); rebuildWidgets(); })
                .bounds(cx + 2, y, hw, BTN_H).build());
        y += STEP + 10;

        // Цвет текста WAILA-блока
        int btnW = w - 28;
        addRow(cx - w / 2, btnW, y, "Цвет названия блока",
                InfoPanelConfig::isShowTargetBlock, InfoPanelConfig::setShowTargetBlock,
                () -> InfoPanelConfig.colorTargetBlock,
                v -> { InfoPanelConfig.colorTargetBlock = v; InfoPanelConfig.save(); });
        y += STEP;

        addCloseButton(cx, y + 4);
    }

    // ════════════════════════════════════════════════════════════════════
    // Вкладка 3 — Структуры
    // ════════════════════════════════════════════════════════════════════
    private void buildTabStructures(int cx) {
        int w = 220;
        int y = CONTENT_Y;

        // Мастер-тогл
        addToggle(cx - w / 2, w, y, "Показывать структуры (синглплейер)",
                InfoPanelConfig::isShowStructures, InfoPanelConfig::setShowStructures);
        y += STEP + 4;

        // Левая колонка — оверворлд
        int colW = (this.width / 2) - 12;
        int btnW = colW - 4;
        int lx = cx - colW - 4;
        int rx = cx + 4;

        addToggle(lx, btnW, y + STEP * 0, "§2Деревня",
                InfoPanelConfig::isShowStructVillage,    InfoPanelConfig::setShowStructVillage);
        addToggle(lx, btnW, y + STEP * 1, "§cАванпост разбойников",
                InfoPanelConfig::isShowStructOutpost,    InfoPanelConfig::setShowStructOutpost);
        addToggle(lx, btnW, y + STEP * 2, "§9Океанский монумент",
                InfoPanelConfig::isShowStructMonument,   InfoPanelConfig::setShowStructMonument);
        addToggle(lx, btnW, y + STEP * 3, "§6Крепость (End)",
                InfoPanelConfig::isShowStructStronghold, InfoPanelConfig::setShowStructStronghold);
        addToggle(lx, btnW, y + STEP * 4, "§eПустынный храм",
                InfoPanelConfig::isShowStructDesert,     InfoPanelConfig::setShowStructDesert);

        // Правая колонка — незер
        addToggle(rx, btnW, y + STEP * 0, "§4Крепость Незера",
                InfoPanelConfig::isShowStructFortress,   InfoPanelConfig::setShowStructFortress);
        addToggle(rx, btnW, y + STEP * 1, "§5Бастион",
                InfoPanelConfig::isShowStructBastion,    InfoPanelConfig::setShowStructBastion);

        addCloseButton(cx, y + STEP * 5 + 4);
    }

    private void addCloseButton(int cx, int y) {
        this.addRenderableWidget(
            Button.builder(Component.literal("Закрыть"),
                btn -> { InfoPanelConfig.save(); this.minecraft.setScreen(parent); })
                .bounds(cx - 50, y, 100, BTN_H).build()
        );
    }

    // ════════════════════════════════════════════════════════════════════
    // Хелперы
    // ════════════════════════════════════════════════════════════════════

    private String pos(Position p) {
        return InfoPanelConfig.getPosition() == p ? "§a" : "§7";
    }

    private String waila(InfoPanelConfig.WailaPosition p) {
        return InfoPanelConfig.getWailaPosition() == p ? "§a" : "§7";
    }

    private void addToggle(int x, int btnW, int y, String label,
                           java.util.function.BooleanSupplier getter,
                           java.util.function.Consumer<Boolean> setter) {
        this.addRenderableWidget(Button.builder(makeLabel(label, getter.getAsBoolean()), btn -> {
            setter.accept(!getter.getAsBoolean());
            btn.setMessage(makeLabel(label, getter.getAsBoolean()));
        }).bounds(x, y, btnW, BTN_H).build());
    }

    private void addRow(int x, int btnW, int y, String label,
                        java.util.function.BooleanSupplier getter,
                        java.util.function.Consumer<Boolean> setter,
                        java.util.function.IntSupplier colorGetter,
                        java.util.function.IntConsumer colorSetter) {
        this.addRenderableWidget(Button.builder(makeLabel(label, getter.getAsBoolean()), btn -> {
            setter.accept(!getter.getAsBoolean());
            btn.setMessage(makeLabel(label, getter.getAsBoolean()));
        }).bounds(x, y, btnW, BTN_H).build());
        this.addRenderableWidget(new ColorButton(x + btnW + 2, y, 22, BTN_H,
                colorGetter::getAsInt,
                btn -> colorSetter.accept(cycleColor(colorGetter.getAsInt()))));
    }

    private Component makeLabel(String label, boolean enabled) {
        return Component.literal((enabled ? "§a✔" : "§c✘") + " §f" + label);
    }

    private int cycleColor(int current) {
        for (int i = 0; i < COLORS.length; i++)
            if (COLORS[i] == current) return COLORS[(i + 1) % COLORS.length];
        return COLORS[0];
    }

    // ════════════════════════════════════════════════════════════════════
    // Рендер
    // ════════════════════════════════════════════════════════════════════

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Заголовок
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 3, 0xFFFFFF);

        // Разделитель под вкладками
        int lineY = TAB_Y + TAB_H + 3;
        graphics.fill(10, lineY, this.width - 10, lineY + 1, 0x88AAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
