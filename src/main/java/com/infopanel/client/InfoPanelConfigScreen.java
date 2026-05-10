package com.infopanel.client;

import com.infopanel.config.InfoPanelConfig;
import com.infopanel.config.InfoPanelConfig.Position;
import com.infopanel.event.Lang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class InfoPanelConfigScreen extends Screen {

    private final Screen parent;
    private int activeTab = 0;

    // ── Layout ────────────────────────────────────────────────────────
    private static final int NAV_W    = 88;   // ширина левой панели навигации
    private static final int NAV_PAD  = 6;    // отступ внутри навигации
    private static final int NAV_BTN_H = 22;  // высота кнопки навигации
    private static final int NAV_GAP  = 2;    // зазор между кнопками навигации
    private static final int CONTENT_X_OFF = NAV_W + 8; // отступ контента от левого края
    private static final int BTN_H    = 20;
    private static final int STEP     = 24;
    private static final int SLIDER_H = 20;

    // Цвета UI
    private static final int COL_NAV_BG      = 0xCC1A1A1A;
    private static final int COL_NAV_ACTIVE  = 0xFF2255AA;
    private static final int COL_NAV_HOVER   = 0x882255AA;
    private static final int COL_SECTION_LINE= 0x88AAAAAA;
    private static final int COL_SECTION_TXT = 0xFFAAAA55;
    private static final int COL_CONTENT_BG  = 0x881A1A1A;

    private static final int[] COLORS = {
        0xFFFFFF, 0xAAAAAA, 0x555555,
        0xFF5555, 0xFF9900, 0xFFFF55,
        0x55FF55, 0x00AA00,
        0x55FFFF, 0x0099FF, 0x0000AA,
        0xFF55FF, 0xAA00AA,
    };

    private static boolean ru() { return Lang.isRussian(); }
    private static String t(String key) {
        return net.minecraft.client.resources.language.I18n.get(key);
    }

    // Категории навигации
    private static final String[] CAT_KEYS = {
        "infopanel.config.tab.panel",
        "infopanel.config.tab.rows",
        "infopanel.config.tab.waila",
        "infopanel.config.tab.structures",
        "infopanel.config.tab.sounds"
    };
    // Иконки категорий (символы)
    private static final String[] CAT_ICONS = { "⚙", "☰", "◎", "⬡", "♪" };

    public InfoPanelConfigScreen(Screen parent) {
        super(Component.literal("InfoPanel"));
        this.parent = parent;
        InfoPanelConfig.load();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Y или Esc — закрыть
        if (keyCode == 256 || keyCode == GLFW.GLFW_KEY_Y) {
            InfoPanelConfig.save();
            this.minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Не рисуем стандартный MC фон — рисуем свой в render()
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ── Вычисляем рабочую область контента ───────────────────────────
    private static final int CONTENT_MAX_W = 300;
    private int contentX() { return CONTENT_X_OFF + 4; }
    private int contentY() { return 42; }  // ниже заголовка вкладки
    private int contentW() { return Math.min(this.width - CONTENT_X_OFF - 8, CONTENT_MAX_W); }

    @Override
    protected void init() {
        sectionHeaders.clear();

        // ── Кнопки навигации (левая панель) ──────────────────────────
        int navY = contentY() + 4;
        for (int i = 0; i < CAT_KEYS.length; i++) {
            final int idx = i;
            String label = CAT_ICONS[i] + " " + t(CAT_KEYS[i]).trim();
            this.addRenderableWidget(
                Button.builder(Component.literal(activeTab == idx ? "§e" + label : "§7" + label), btn -> {
                    activeTab = idx;
                    rebuildWidgets();
                }).bounds(NAV_PAD, navY + idx * (NAV_BTN_H + NAV_GAP), NAV_W - NAV_PAD * 2, NAV_BTN_H).build()
            );
        }

        // ── Контент активной вкладки ──────────────────────────────────
        switch (activeTab) {
            case 0 -> buildPanel();
            case 1 -> buildRows();
            case 2 -> buildWaila();
            case 3 -> buildStructures();
            case 4 -> buildSounds();
        }

        // ── Кнопка Закрыть внизу ─────────────────────────────────────
        this.addRenderableWidget(
            Button.builder(Component.literal(ru() ? "§7✖  Закрыть" : "§7✖  Close"),
                btn -> { InfoPanelConfig.save(); this.minecraft.setScreen(parent); })
                .bounds(NAV_PAD, this.height - 28, NAV_W - NAV_PAD * 2, 20).build()
        );
    }

    // ════════════════════════════════════════════════════════════════════
    // Вкладка 0 — Панель
    // ════════════════════════════════════════════════════════════════════
    private void buildPanel() {
        int x = contentX();
        int w = contentW();
        int y = contentY() + 8;

        // Секция: Внешний вид
        y = sectionY(y); // резервируем место под заголовок секции
        y += 4;

        // Слайдер фона
        addSlider(x, y, w, "infopanel.config.panel.bg",
            InfoPanelConfig.getBgAlpha() / 255.0,
            v -> InfoPanelConfig.setBgAlpha((int)(v * 255)),
            v -> t("infopanel.config.panel.bg") + (int)(v * 255));
        y += STEP;

        // Слайдер масштаба
        addSlider(x, y, w, "infopanel.config.panel.scale",
            (InfoPanelConfig.getScale() - 0.5) / 1.5,
            v -> InfoPanelConfig.setScale((float)(0.5 + v * 1.5)),
            v -> String.format(t("infopanel.config.panel.scale") + "%.1f×", 0.5 + v * 1.5));
        y += STEP + 8;

        // Секция: Позиция
        y = sectionY(y);
        y += 4;

        int hw = w / 2 - 2;
        addPosBtn(x,        y, hw, Position.TOP_LEFT,    t("infopanel.config.panel.topleft"));
        addPosBtn(x + hw + 2, y, hw, Position.TOP_RIGHT,  t("infopanel.config.panel.topright"));
        y += STEP;
        addPosBtn(x,        y, hw, Position.BOTTOM_LEFT,  t("infopanel.config.panel.bottomleft"));
        addPosBtn(x + hw + 2, y, hw, Position.BOTTOM_RIGHT, t("infopanel.config.panel.bottomright"));
        y += STEP + 4;

        this.addRenderableWidget(Button.builder(
            Component.literal("§b" + t("infopanel.config.panel.edit")),
            btn -> this.minecraft.setScreen(new PanelEditScreen()))
            .bounds(x, y, w, BTN_H).build());
        y += STEP + 8;

        // Секция: Сброс
        y = sectionY(y);
        y += 4;

        int rw = (w - 4) / 3;
        this.addRenderableWidget(Button.builder(
            Component.literal("§7↺ " + t("infopanel.config.panel.reset.panel")),
            btn -> { InfoPanelConfig.panelX = -1; InfoPanelConfig.panelY = -1; InfoPanelConfig.save(); })
            .bounds(x, y, rw, BTN_H).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("§7↺ " + t("infopanel.config.panel.reset.compass")),
            btn -> { InfoPanelConfig.compassX = -1; InfoPanelConfig.compassY = -1; InfoPanelConfig.save(); })
            .bounds(x + rw + 2, y, rw, BTN_H).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("§7↺ " + t("infopanel.config.panel.reset.dur")),
            btn -> { InfoPanelConfig.durabilityX = -1; InfoPanelConfig.durabilityY = -1; InfoPanelConfig.save(); })
            .bounds(x + (rw + 2) * 2, y, rw, BTN_H).build());
    }

    private void addPosBtn(int x, int y, int w, Position p, String label) {
        boolean active = InfoPanelConfig.getPosition() == p;
        this.addRenderableWidget(Button.builder(
            Component.literal((active ? "§a● " : "§7○ ") + label),
            btn -> { InfoPanelConfig.setPosition(p); rebuildWidgets(); })
            .bounds(x, y, w, BTN_H).build());
    }

    // ════════════════════════════════════════════════════════════════════
    // Вкладка 1 — Строки HUD
    // ════════════════════════════════════════════════════════════════════
    private void buildRows() {
        int x = contentX();
        int y = contentY();

        // Вычисляем максимальную ширину кнопки левой колонки
        // (кнопка + ColorButton 22px + зазор 2px = +24)
        String[] leftLabels = {
            t("infopanel.config.rows.coords"),
            t("infopanel.config.rows.direction"),
            t("infopanel.config.rows.biome"),
            t("infopanel.config.rows.time"),
            t("infopanel.config.rows.session"),
            t("infopanel.config.rows.durability"),
            t("infopanel.config.rows.players")
        };
        int maxLeftBtnW = 0;
        for (String lbl : leftLabels) {
            int bw = this.font.width(lbl) + 28;
            if (bw > maxLeftBtnW) maxLeftBtnW = bw;
        }
        // Правая колонка начинается после самой широкой кнопки + ColorButton + отступ
        int lx = x;
        int rx = x + maxLeftBtnW + 24 + 8; // кнопка + ColorButton(22) + зазор(2) + отступ(8)
        int ly = y, ry = y;

        // Левая колонка
        addRow(lx, ly, t("infopanel.config.rows.coords"),
            InfoPanelConfig::isShowCoords, InfoPanelConfig::setShowCoords,
            () -> InfoPanelConfig.colorCoords, v -> { InfoPanelConfig.colorCoords = v; InfoPanelConfig.save(); });
        ly += STEP;
        addRow(lx, ly, t("infopanel.config.rows.direction"),
            InfoPanelConfig::isShowDirection, InfoPanelConfig::setShowDirection,
            () -> InfoPanelConfig.colorDirection, v -> { InfoPanelConfig.colorDirection = v; InfoPanelConfig.save(); });
        ly += STEP;
        addRow(lx, ly, t("infopanel.config.rows.biome"),
            InfoPanelConfig::isShowBiome, InfoPanelConfig::setShowBiome,
            () -> InfoPanelConfig.colorBiome, v -> { InfoPanelConfig.colorBiome = v; InfoPanelConfig.save(); });
        ly += STEP;
        addRow(lx, ly, t("infopanel.config.rows.time"),
            InfoPanelConfig::isShowTime, InfoPanelConfig::setShowTime,
            () -> InfoPanelConfig.colorTime, v -> { InfoPanelConfig.colorTime = v; InfoPanelConfig.save(); });
        ly += STEP;
        addRow(lx, ly, t("infopanel.config.rows.session"),
            InfoPanelConfig::isShowSession, InfoPanelConfig::setShowSession,
            () -> InfoPanelConfig.colorSession, v -> { InfoPanelConfig.colorSession = v; InfoPanelConfig.save(); });
        ly += STEP;
        addRow(lx, ly, t("infopanel.config.rows.durability"),
            InfoPanelConfig::isShowDurability, InfoPanelConfig::setShowDurability,
            () -> InfoPanelConfig.colorDurability, v -> { InfoPanelConfig.colorDurability = v; InfoPanelConfig.save(); });
        ly += STEP;
        addRow(lx, ly, t("infopanel.config.rows.players"),
            InfoPanelConfig::isShowPlayers, InfoPanelConfig::setShowPlayers,
            () -> InfoPanelConfig.colorPlayers, v -> { InfoPanelConfig.colorPlayers = v; InfoPanelConfig.save(); });

        // Правая колонка
        addRow(rx, ry, "FPS",
            InfoPanelConfig::isShowFps, InfoPanelConfig::setShowFps,
            () -> InfoPanelConfig.colorFps, v -> { InfoPanelConfig.colorFps = v; InfoPanelConfig.save(); });
        ry += STEP;
        addRow(rx, ry, t("infopanel.config.rows.ping"),
            InfoPanelConfig::isShowPing, InfoPanelConfig::setShowPing,
            () -> InfoPanelConfig.colorPing, v -> { InfoPanelConfig.colorPing = v; InfoPanelConfig.save(); });
        ry += STEP;
        addRow(rx, ry, "TPS",
            InfoPanelConfig::isShowTps, InfoPanelConfig::setShowTps,
            () -> InfoPanelConfig.colorTps, v -> { InfoPanelConfig.colorTps = v; InfoPanelConfig.save(); });
        ry += STEP;
        addToggle(rx, ry, t("infopanel.config.rows.effects"),
            InfoPanelConfig::isShowEffectTimers, InfoPanelConfig::setShowEffectTimers);
        ry += STEP;
        addToggle(rx, ry, t("infopanel.config.rows.compass"),
            InfoPanelConfig::isShowCompassBar, InfoPanelConfig::setShowCompassBar);
        ry += STEP;
        addToggle(rx, ry, t("infopanel.config.rows.light"),
            InfoPanelConfig::isShowLightOverlay, InfoPanelConfig::setShowLightOverlay);
        ry += STEP;
        addToggle(rx, ry, t("infopanel.config.rows.slime"),
            InfoPanelConfig::isShowSlimeChunks, InfoPanelConfig::setShowSlimeChunks);
        ry += STEP + 8;

        // Секция: Лог предметов
        int fullW = contentW();
        addToggle(x, ry, t("infopanel.config.rows.pickuplog"),
            InfoPanelConfig::isShowPickupLog, InfoPanelConfig::setShowPickupLog);
        ry += STEP;

        // Слайдер фона лога
        addSlider(x, ry, Math.min(fullW, 200), "infopanel.config.rows.pickuplog.bg",
            InfoPanelConfig.getPickupLogBgAlpha() / 255.0,
            v -> InfoPanelConfig.setPickupLogBgAlpha((int)(v * 255)),
            v -> t("infopanel.config.rows.pickuplog.bg") + (int)(v * 255));
        ry += STEP;

        // Слайдер количества предметов (1–20)
        addSlider(x, ry, Math.min(fullW, 200), "infopanel.config.rows.pickuplog.max",
            (InfoPanelConfig.getPickupLogMaxItems() - 1) / 19.0,
            v -> InfoPanelConfig.setPickupLogMaxItems((int)(1 + v * 19)),
            v -> t("infopanel.config.rows.pickuplog.max") + (int)(1 + v * 19));
        ry += STEP;

        // Слайдер времени показа (2–20 сек)
        addSlider(x, ry, Math.min(fullW, 200), "infopanel.config.rows.pickuplog.lifetime",
            (InfoPanelConfig.getPickupLogLifetime() - 2) / 18.0,
            v -> InfoPanelConfig.setPickupLogLifetime((int)(2 + v * 18)),
            v -> t("infopanel.config.rows.pickuplog.lifetime") + (int)(2 + v * 18) + "s");
        ry += STEP;

        // Слайдер масштаба (0.5–2.0)
        addSlider(x, ry, Math.min(fullW, 200), "infopanel.config.rows.pickuplog.scale",
            (InfoPanelConfig.getPickupLogScale() - 0.5) / 1.5,
            v -> InfoPanelConfig.setPickupLogScale((float)(0.5 + v * 1.5)),
            v -> String.format(t("infopanel.config.rows.pickuplog.scale") + "%.1f\u00d7", 0.5 + v * 1.5));
        ry += STEP;

        this.addRenderableWidget(Button.builder(
            Component.literal("§7↺ " + t("infopanel.config.rows.pickuplog.reset")),
            btn -> InfoPanelConfig.setPickupLogPos(-1, -1))
            .bounds(x, ry, this.font.width(t("infopanel.config.rows.pickuplog.reset")) + 28, BTN_H).build());
    }

    // ════════════════════════════════════════════════════════════════════
    // Вкладка 2 — WAILA
    // ════════════════════════════════════════════════════════════════════
    private void buildWaila() {
        int x = contentX();
        int w = contentW();
        int y = contentY() + 8;

        // Секция: Внешний вид
        y = sectionY(y); y += 4;

        addSlider(x, y, w, "infopanel.config.waila.bg",
            InfoPanelConfig.getWailaBgAlpha() / 255.0,
            v -> InfoPanelConfig.setWailaBgAlpha((int)(v * 255)),
            v -> t("infopanel.config.waila.bg") + (int)(v * 255));
        y += STEP;

        addRow(x, y, t("infopanel.config.waila.color"),
            InfoPanelConfig::isShowTargetBlock, InfoPanelConfig::setShowTargetBlock,
            () -> InfoPanelConfig.colorTargetBlock,
            v -> { InfoPanelConfig.colorTargetBlock = v; InfoPanelConfig.save(); });
        y += STEP + 8;

        // Секция: Позиция WAILA
        y = sectionY(y); y += 4;

        int hw = w / 2 - 2;
        addWailaBtn(x,        y, hw, InfoPanelConfig.WailaPosition.TOP_CENTER,   t("infopanel.config.waila.topcenter"));
        addWailaBtn(x + hw + 2, y, hw, InfoPanelConfig.WailaPosition.TOP_RIGHT,  t("infopanel.config.waila.topright"));
        y += STEP;
        addWailaBtn(x,        y, hw, InfoPanelConfig.WailaPosition.BOTTOM_CENTER, t("infopanel.config.waila.botcenter"));
        addWailaBtn(x + hw + 2, y, hw, InfoPanelConfig.WailaPosition.TOP_LEFT,   t("infopanel.config.waila.topleft"));
    }

    private void addWailaBtn(int x, int y, int w, InfoPanelConfig.WailaPosition p, String label) {
        boolean active = InfoPanelConfig.getWailaPosition() == p;
        this.addRenderableWidget(Button.builder(
            Component.literal((active ? "§a● " : "§7○ ") + label),
            btn -> { InfoPanelConfig.setWailaPosition(p); rebuildWidgets(); })
            .bounds(x, y, w, BTN_H).build());
    }

    // ════════════════════════════════════════════════════════════════════
    // Вкладка 3 — Структуры
    // ════════════════════════════════════════════════════════════════════
    private void buildStructures() {
        int x = contentX();
        int y = contentY();

        addToggle(x, y, t("infopanel.config.struct.master"),
            InfoPanelConfig::isShowStructures, InfoPanelConfig::setShowStructures);
        y += STEP + 4;
        addToggle(x, y, t("infopanel.config.struct.village"),
            InfoPanelConfig::isShowStructVillage, InfoPanelConfig::setShowStructVillage);
        y += STEP;
        addToggle(x, y, t("infopanel.config.struct.outpost"),
            InfoPanelConfig::isShowStructOutpost, InfoPanelConfig::setShowStructOutpost);
        y += STEP;
        addToggle(x, y, t("infopanel.config.struct.monument"),
            InfoPanelConfig::isShowStructMonument, InfoPanelConfig::setShowStructMonument);
        y += STEP;
        addToggle(x, y, t("infopanel.config.struct.stronghold"),
            InfoPanelConfig::isShowStructStronghold, InfoPanelConfig::setShowStructStronghold);
        y += STEP;
        addToggle(x, y, t("infopanel.config.struct.desert"),
            InfoPanelConfig::isShowStructDesert, InfoPanelConfig::setShowStructDesert);
        y += STEP;
        addToggle(x, y, t("infopanel.config.struct.fortress"),
            InfoPanelConfig::isShowStructFortress, InfoPanelConfig::setShowStructFortress);
        y += STEP;
        addToggle(x, y, t("infopanel.config.struct.bastion"),
            InfoPanelConfig::isShowStructBastion, InfoPanelConfig::setShowStructBastion);
    }

    // ════════════════════════════════════════════════════════════════════
    // Вкладка 4 — Звуки
    // ════════════════════════════════════════════════════════════════════
    private void buildSounds() {
        int x  = contentX();
        int y  = contentY();
        int sw = Math.min(contentW(), 200); // ширина слайдера — не шире 200px

        addToggle(x, y, t("infopanel.config.sounds.ambient"),
            InfoPanelConfig::isPortalSoundEnabled, InfoPanelConfig::setPortalSoundEnabled);
        y += STEP;
        addSlider(x, y, sw, "infopanel.config.sounds.ambient.volume",
            InfoPanelConfig.getPortalSoundVolume(),
            v -> InfoPanelConfig.setPortalSoundVolume(v.floatValue()),
            v -> t("infopanel.config.sounds.ambient.volume") + (int)(v * 100) + "%");
        y += STEP + 8;

        addToggle(x, y, t("infopanel.config.sounds.travel"),
            InfoPanelConfig::isPortalTravelEnabled, InfoPanelConfig::setPortalTravelEnabled);
        y += STEP;
        addSlider(x, y, sw, "infopanel.config.sounds.travel.volume",
            InfoPanelConfig.getPortalTravelVolume(),
            v -> InfoPanelConfig.setPortalTravelVolume(v.floatValue()),
            v -> t("infopanel.config.sounds.travel.volume") + (int)(v * 100) + "%");
    }

    // ════════════════════════════════════════════════════════════════════
    // Хелперы виджетов
    // ════════════════════════════════════════════════════════════════════

    /** Возвращает Y после заголовка секции (сам заголовок рисуется в render) */
    private int sectionY(int y) {
        sectionHeaders.add(y);
        return y + 12; // высота заголовка
    }

    // Список Y-координат заголовков секций для рендера
    private final java.util.List<Integer> sectionHeaders = new java.util.ArrayList<>();
    // Список названий секций
    private final java.util.List<String> sectionNames = new java.util.ArrayList<>();

    private void addSlider(int x, int y, int w, String labelKey,
                           double initVal,
                           Consumer<Double> setter,
                           java.util.function.Function<Double, String> msgFn) {
        String initMsg = msgFn.apply(initVal);
        this.addRenderableWidget(new AbstractSliderButton(x, y, w, SLIDER_H,
                Component.literal(initMsg), initVal) {
            @Override protected void updateMessage() {
                setter.accept(value);
                setMessage(Component.literal(msgFn.apply(value)));
            }
            @Override protected void applyValue() { setter.accept(value); }
        });
    }

    private void addToggle(int x, int y, String label,
                           BooleanSupplier getter, Consumer<Boolean> setter) {
        // Ширина = ширина текста + паддинг кнопки (8px с каждой стороны)
        String fullLabel = (getter.getAsBoolean() ? "§a✔ §f" : "§c✘ §7") + label;
        int bw = this.font.width(label) + 28; // 28 = иконка + паддинги
        this.addRenderableWidget(Button.builder(makeToggleLabel(label, getter.getAsBoolean()), btn -> {
            setter.accept(!getter.getAsBoolean());
            btn.setMessage(makeToggleLabel(label, getter.getAsBoolean()));
        }).bounds(x, y, bw, BTN_H).build());
    }

    private void addRow(int x, int y, String label,
                        BooleanSupplier getter, Consumer<Boolean> setter,
                        IntSupplier colorGetter, IntConsumer colorSetter) {
        int bw = this.font.width(label) + 28;
        this.addRenderableWidget(Button.builder(makeToggleLabel(label, getter.getAsBoolean()), btn -> {
            setter.accept(!getter.getAsBoolean());
            btn.setMessage(makeToggleLabel(label, getter.getAsBoolean()));
        }).bounds(x, y, bw, BTN_H).build());
        this.addRenderableWidget(new ColorButton(x + bw + 2, y, 22, BTN_H,
                colorGetter::getAsInt,
                btn -> colorSetter.accept(cycleColor(colorGetter.getAsInt()))));
    }

    private Component makeToggleLabel(String label, boolean enabled) {
        return Component.literal((enabled ? "§a✔ §f" : "§c✘ §7") + label);
    }

    private int cycleColor(int current) {
        for (int i = 0; i < COLORS.length; i++)
            if (COLORS[i] == current) return COLORS[(i + 1) % COLORS.length];
        return COLORS[0];
    }

    // ════════════════════════════════════════════════════════════════════
    // Рендер
    // ════════════════════════════════════════════════════════════════════

    // Названия секций для каждой вкладки
    private static final String[][] SECTION_NAMES = {
        // Панель
        { "infopanel.section.appearance", "infopanel.section.position", "infopanel.section.reset" },
        // Строки
        { "infopanel.section.info", "infopanel.section.perf", "infopanel.section.overlays" },
        // WAILA
        { "infopanel.section.appearance", "infopanel.section.position" },
        // Структуры
        { "infopanel.section.general", "infopanel.section.overworld", "infopanel.section.nether" },
        // Звуки
        { "infopanel.section.portal_hum", "infopanel.section.portal_travel" }
    };

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Полноэкранный тёмный фон (заменяет renderBackground)
        g.fill(0, 0, this.width, this.height, 0xCC000000);

        // ── Левая навигационная панель ────────────────────────────────
        g.fill(0, 0, NAV_W, this.height, COL_NAV_BG);
        // Разделитель
        g.fill(NAV_W, 0, NAV_W + 1, this.height, 0x55AAAAAA);

        // Заголовок мода в навигации
        g.drawCenteredString(this.font, "§bInfoPanel", NAV_W / 2, 8, 0xFFFFFF);
        g.fill(NAV_PAD, 18, NAV_W - NAV_PAD, 19, 0x44AAAAAA);

        // Подсветка активной категории
        int navY = contentY() + 4;
        for (int i = 0; i < CAT_KEYS.length; i++) {
            if (i == activeTab) {
                g.fill(NAV_PAD - 2, navY + i * (NAV_BTN_H + NAV_GAP) - 1,
                       NAV_W - NAV_PAD + 2, navY + i * (NAV_BTN_H + NAV_GAP) + NAV_BTN_H + 1,
                       COL_NAV_ACTIVE);
            }
        }

        // ── Область контента ─────────────────────────────────────────
        g.fill(NAV_W + 1, 0, this.width, this.height, COL_CONTENT_BG);

        // Заголовок активной вкладки
        String tabTitle = t(CAT_KEYS[activeTab]).trim();
        g.drawString(this.font, "§e" + CAT_ICONS[activeTab] + " §f" + tabTitle,
            contentX(), 28, 0xFFFFFF, true);

        // ── Заголовки секций ─────────────────────────────────────────
        String[] secNames = activeTab < SECTION_NAMES.length ? SECTION_NAMES[activeTab] : new String[0];
        for (int i = 0; i < sectionHeaders.size(); i++) {
            int sy = sectionHeaders.get(i);
            String secKey = i < secNames.length ? secNames[i] : "";
            String secLabel = secKey.isEmpty() ? "" : t(secKey);
            if (!secLabel.isEmpty()) {
                // Только текст секции — без линии
                g.drawString(this.font, "§6" + secLabel, contentX(), sy + 1, COL_SECTION_TXT, false);
                // Короткая линия только справа от текста
                int tw = this.font.width(secLabel) + contentX() + 6;
                g.fill(tw, sy + 5, contentX() + contentW(), sy + 6, 0x44AAAAAA);
            }
        }

        super.render(g, mouseX, mouseY, partialTick);
    }
}
