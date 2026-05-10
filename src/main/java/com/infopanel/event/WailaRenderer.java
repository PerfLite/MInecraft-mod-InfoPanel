package com.infopanel.event;

import com.infopanel.config.InfoPanelConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.Map;
import java.util.HashMap;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = "infopanel", value = Dist.CLIENT)
public class WailaRenderer {

    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        if (!InfoPanelConfig.hudVisible) return;
        if (!InfoPanelConfig.isShowTargetBlock()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return;
        if (mc.player.isSpectator()) return;
        if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)
                && !(mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;

        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return;

        String name = null;
        ItemStack icon = ItemStack.EMPTY;
        String extra = null;
        String extra2 = null;
        java.util.List<ItemStack> furnaceItems = null;
        int[] furnaceData = null;
        String blockId = "";

        if (hit instanceof BlockHitResult blockHit) {
            BlockPos targetPos = blockHit.getBlockPos();
            BlockState state = mc.level.getBlockState(targetPos);
            if (state.isAir()) return;
            name = state.getBlock().getName().getString();
            icon = new ItemStack(state.getBlock().asItem());

            // ── Руды: диапазон Y-спавна ──────────────────────────────────
            blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getKey(state.getBlock()).toString();
            String spawnRange = getOreSpawnRange(blockId);
            if (spawnRange != null) {
                extra = "Y: " + spawnRange;
            }

            // ── Растения: уровень созревания ─────────────────────────────
            net.minecraft.world.level.block.state.properties.IntegerProperty ageProp = null;
            for (var prop : state.getProperties()) {
                if (prop.getName().equals("age")
                        && prop instanceof net.minecraft.world.level.block.state.properties.IntegerProperty ip) {
                    ageProp = ip;
                    break;
                }
            }
            if (ageProp != null) {
                int age    = state.getValue(ageProp);
                int maxAge = ageProp.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(age);
                boolean ripe = age >= maxAge;
                String status = ripe ? net.minecraft.client.resources.language.I18n.get("infopanel.waila.ripe") : "";
                extra = net.minecraft.client.resources.language.I18n.get("infopanel.waila.growth")
                        + age + " : " + maxAge + status;
            }

            // ── Редстоун-провод: сила сигнала ────────────────────────────
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER)) {
                int power = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER);
                extra = net.minecraft.client.resources.language.I18n.get("infopanel.waila.signal")
                        + power + " : 15";
            }

            // ── Повторитель: задержка ────────────────────────────────────
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DELAY)) {
                int delay = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DELAY);
                String powered = state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)
                    && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)
                    ? net.minecraft.client.resources.language.I18n.get("infopanel.waila.on")
                    : net.minecraft.client.resources.language.I18n.get("infopanel.waila.off");
                String tickKey = delay > 1 ? "infopanel.waila.delay.ticks" : "infopanel.waila.delay.tick";
                extra = net.minecraft.client.resources.language.I18n.get("infopanel.waila.delay")
                        + delay
                        + net.minecraft.client.resources.language.I18n.get(tickKey)
                        + powered;
            }

            // ── Компаратор: режим и сигнал ───────────────────────────────
            if (blockId.contains("comparator")) {
                boolean on = state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)
                    && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED);
                String mode = "";
                for (var prop : state.getProperties()) {
                    if (prop.getName().equals("mode")) {
                        mode = state.getValue(prop).toString();
                        break;
                    }
                }
                String modeKey = mode.equals("subtract") ? "infopanel.waila.subtract" : "infopanel.waila.compare";
                String onKey   = on ? "infopanel.waila.on" : "infopanel.waila.off";
                int outPower = mc.level.getSignal(targetPos,
                    state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING));
                extra = net.minecraft.client.resources.language.I18n.get(modeKey)
                        + net.minecraft.client.resources.language.I18n.get(onKey)
                        + "  "
                        + net.minecraft.client.resources.language.I18n.get("infopanel.waila.signal")
                        + outPower + " : 15";
            }

            // ── Печи / плавильни / коптильни ─────────────────────────────
            if (mc.getSingleplayerServer() != null) {
                var sl = mc.getSingleplayerServer().getLevel(mc.level.dimension());
                if (sl != null) {
                    try {
                        java.util.concurrent.CompletableFuture<Object[]> future =
                            new java.util.concurrent.CompletableFuture<>();
                        mc.getSingleplayerServer().execute(() -> {
                            try {
                                sl.getChunkAt(targetPos);
                                var be2 = sl.getBlockEntity(targetPos);
                                if (be2 instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity f) {
                                    net.minecraft.world.item.ItemStack inp = f.getItem(0).copy();
                                    net.minecraft.world.item.ItemStack fuel= f.getItem(1).copy();
                                    net.minecraft.world.item.ItemStack res = f.getItem(2).copy();
                                    int cp = getIntField(f, "cookingTimer");
                                    if (cp == 0) cp = getIntField(f, "cookingProgress");
                                    int ct = getIntField(f, "cookingTotalTime");
                                    int lt = getIntField(f, "litTime");
                                    int ld = getIntField(f, "litDuration");
                                    future.complete(new Object[]{inp, fuel, res, cp, ct, lt, ld});
                                } else {
                                    future.complete(null);
                                }
                            } catch (Exception e) { future.complete(null); }
                        });
                        Object[] r = future.get(50, java.util.concurrent.TimeUnit.MILLISECONDS);
                        if (r != null) {
                            furnaceItems = java.util.List.of(
                                (net.minecraft.world.item.ItemStack) r[0],
                                (net.minecraft.world.item.ItemStack) r[1],
                                (net.minecraft.world.item.ItemStack) r[2]);
                            furnaceData = new int[]{(int)r[3], (int)r[4], (int)r[5], (int)r[6]};
                            icon = ItemStack.EMPTY;
                        }
                    } catch (Exception ignored) {}
                }
            }

            // ── Улей / пчелиное гнездо ───────────────────────────────────
            if (blockId.contains("beehive") || blockId.contains("bee_nest")) {
                int honey = state.getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL);
                int bees = 0;
                boolean full = false;
                int maxBees = net.minecraft.world.level.block.entity.BeehiveBlockEntity.MAX_OCCUPANTS;

                // Читаем с серверного BE в server thread через submit
                if (mc.getSingleplayerServer() != null) {
                    var sl = mc.getSingleplayerServer().getLevel(mc.level.dimension());
                    if (sl != null) {
                        try {
                            // Выполняем в server thread синхронно
                            java.util.concurrent.CompletableFuture<int[]> future =
                                new java.util.concurrent.CompletableFuture<>();
                            mc.getSingleplayerServer().execute(() -> {
                                try {
                                    sl.getChunkAt(targetPos);
                                    var be2 = sl.getBlockEntity(targetPos);
                                    var ss  = sl.getBlockState(targetPos);
                                    int h = ss.getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL);
                                    int b = 0;
                                    if (be2 instanceof net.minecraft.world.level.block.entity.BeehiveBlockEntity hive2) {
                                        b = hive2.getOccupantCount();
                                    }
                                    future.complete(new int[]{h, b});
                                } catch (Exception e) {
                                    future.complete(new int[]{0, 0});
                                }
                            });
                            // Ждём максимум 50мс чтобы не фризить рендер
                            int[] result = future.get(50, java.util.concurrent.TimeUnit.MILLISECONDS);
                            honey = result[0];
                            bees  = result[1];
                            full  = bees >= maxBees;
                        } catch (Exception ignored) {}
                    }
                }

                String honeyColor = honey == 5 ? "§a" : "§e";
                String beesColor  = full ? "§a" : "§b";
                extra  = net.minecraft.client.resources.language.I18n.get("infopanel.waila.beehive.honey")
                        + honeyColor + honey + "§7/5";
                extra2 = net.minecraft.client.resources.language.I18n.get("infopanel.waila.beehive.bees")
                        + beesColor + bees + "§7/" + maxBees;
            }

            // ── Котлы ────────────────────────────────────────────────────
            if (state.getBlock() instanceof net.minecraft.world.level.block.CauldronBlock) {
                extra = net.minecraft.client.resources.language.I18n.get("infopanel.waila.cauldron.empty");
            } else if (state.getBlock() instanceof net.minecraft.world.level.block.LayeredCauldronBlock) {
                int lvl = state.getValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL);
                // Определяем тип по id блока
                String typeKey;
                if (blockId.contains("powder_snow")) {
                    typeKey = "infopanel.waila.cauldron.snow";
                } else if (blockId.contains("lava")) {
                    typeKey = "infopanel.waila.cauldron.lava";
                } else {
                    typeKey = "infopanel.waila.cauldron.water";
                }
                extra = net.minecraft.client.resources.language.I18n.get(typeKey) + lvl + " : 3";
            } else if (blockId.contains("lava_cauldron")) {
                extra = net.minecraft.client.resources.language.I18n.get("infopanel.waila.cauldron.lava");
            }

            // ── Координаты портала ───────────────────────────────────────
            if (blockId.contains("nether_portal") || blockId.contains("end_portal")) {
                var dim = mc.level.dimension();
                double px = mc.player.getX();
                double pz = mc.player.getZ();
                if (dim.equals(net.minecraft.world.level.Level.OVERWORLD)) {
                    int nx = (int)(px / 8);
                    int nz = (int)(pz / 8);
                    extra = net.minecraft.client.resources.language.I18n.get("infopanel.waila.portal.nether")
                            + "X: " + nx + "  Z: " + nz;
                } else if (dim.equals(net.minecraft.world.level.Level.NETHER)) {
                    int ox = (int)(px * 8);
                    int oz = (int)(pz * 8);
                    extra = net.minecraft.client.resources.language.I18n.get("infopanel.waila.portal.overworld")
                            + "X: " + ox + "  Z: " + oz;
                }
            }

        } else if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            icon = ItemStack.EMPTY;
            if (entity instanceof net.minecraft.world.entity.npc.Villager villager) {
                // Читаем уровень с серверной сущности — она всегда актуальна
                int level = villager.getVillagerData().getLevel();
                String profKey = villager.getVillagerData().getProfession().name();

                if (mc.getSingleplayerServer() != null) {
                    var sl = mc.getSingleplayerServer().getLevel(mc.level.dimension());
                    if (sl != null) {
                        // Читаем напрямую без Future — сервер в том же процессе
                        var serverEntity = sl.getEntity(villager.getUUID());
                        if (serverEntity instanceof net.minecraft.world.entity.npc.Villager sv) {
                            level   = sv.getVillagerData().getLevel();
                            profKey = sv.getVillagerData().getProfession().name();
                        }
                    }
                }
                // Цвет по уровню: 1=серый, 2=белый, 3=зелёный, 4=голубой, 5=золотой
                String levelColor = switch (level) {
                    case 1 -> "§7";
                    case 2 -> "§f";
                    case 3 -> "§a";
                    case 4 -> "§b";
                    default -> "§6";
                };
                // Используем MC-ключ для профессии — entity.minecraft.villager.<prof>
                // Если ключ не найден (I18n вернёт сам ключ) — используем entity.getName()
                String profTranslated = net.minecraft.client.resources.language.I18n.get(
                        "entity.minecraft.villager." + profKey);
                // Если перевод не найден — I18n вернёт ключ как есть
                if (profTranslated.equals("entity.minecraft.villager." + profKey)) {
                    // Fallback: берём имя напрямую из MC (оно уже переведено)
                    profTranslated = entity.getName().getString();
                }
                name = levelColor + profTranslated;
                int hp    = (int) villager.getHealth();
                int maxHp = (int) villager.getMaxHealth();
                String levelKey = switch (level) {
                    case 1 -> "infopanel.waila.villager.novice";
                    case 2 -> "infopanel.waila.villager.apprentice";
                    case 3 -> "infopanel.waila.villager.journeyman";
                    case 4 -> "infopanel.waila.villager.expert";
                    default -> "infopanel.waila.villager.master";
                };
                extra = "❤ " + hp + " : " + maxHp
                        + "  §7("
                        + net.minecraft.client.resources.language.I18n.get(levelKey)
                        + ")";
            } else if (entity instanceof LivingEntity living) {
                name = entity.getName().getString();
                int hp    = (int) living.getHealth();
                int maxHp = (int) living.getMaxHealth();
                extra = "❤ " + hp + " : " + maxHp;
            } else {
                name = entity.getName().getString();
            }
        }

        if (name == null) return;

        // Для мобов передаём саму сущность для рендера портрета
        LivingEntity entityForPortrait = null;
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity le) {
            entityForPortrait = le;
        }

        renderWaila(event.getGuiGraphics(), mc, name, icon, extra, extra2, entityForPortrait, furnaceItems, furnaceData, blockId);
    }

    private static void renderWaila(GuiGraphics graphics, Minecraft mc,
                                    String name, ItemStack icon, String extra, String extra2,
                                    LivingEntity entityForPortrait,
                                    java.util.List<ItemStack> furnaceItems, int[] furnaceData,
                                    String blockId) {
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        final int ICON = 16;
        final int PAD  = 5;
        final int PORT = 32;

        boolean hasIcon     = !icon.isEmpty();
        boolean hasPortrait = entityForPortrait != null;
        boolean hasExtra    = extra  != null;
        boolean hasExtra2   = extra2 != null;
        boolean hasFurnace  = furnaceItems != null && furnaceData != null;

        int totalW, totalH;

        if (hasFurnace) {
            int nameW = ICON + 4 + mc.font.width(name);
            int rowW  = ICON + 20 + ICON + 8 + ICON + 4 + mc.font.width("x64");
            totalW = PAD + Math.max(nameW, rowW) + PAD;
            totalH = PAD + ICON + 4 + ICON + PAD;
        } else {
            int leftColW = 0;
            if (hasIcon)     leftColW = ICON + PAD;
            if (hasPortrait) leftColW = PORT + PAD;
            int textW   = mc.font.width(name);
            int extraW  = hasExtra  ? mc.font.width(extra)  : 0;
            int extra2W = hasExtra2 ? mc.font.width(extra2) : 0;
            int contentW = Math.max(textW, Math.max(extraW, extra2W));
            totalW = PAD + leftColW + contentW + PAD;
            int lines = 1 + (hasExtra ? 1 : 0) + (hasExtra2 ? 1 : 0);
            totalH = PAD + mc.font.lineHeight * lines + (lines - 1) * 3 + PAD;
            if (hasPortrait) totalH = Math.max(totalH, PAD + PORT + PAD);
        }

        int margin = 6, hotbarH = 44;
        int x, y;
        switch (InfoPanelConfig.getWailaPosition()) {
            case BOTTOM_CENTER -> { x = (screenW - totalW) / 2; y = screenH - totalH - hotbarH; }
            case TOP_LEFT      -> { x = margin;                  y = margin; }
            case TOP_RIGHT     -> { x = screenW - totalW - margin; y = margin; }
            default            -> { x = (screenW - totalW) / 2; y = margin; }
        }

        int alpha = InfoPanelConfig.getWailaBgAlpha();
        if (alpha > 0) {
            graphics.fill(x - 1, y - 1, x + totalW + 1, y + totalH + 1, 0x55000000);
            graphics.fill(x, y, x + totalW, y + totalH, (alpha << 24) | 0x000000);
        }

        int nameColor = 0xFF000000 | InfoPanelConfig.colorTargetBlock;

        // ── Режим печи ───────────────────────────────────────────────────
        if (hasFurnace) {
            // Строка 1: иконка печи + название
            graphics.renderItem(new net.minecraft.world.item.ItemStack(
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                    net.minecraft.resources.ResourceLocation.parse(blockId))), x + PAD, y + PAD);
            graphics.drawString(mc.font, name, x + PAD + ICON + 4, y + PAD + (ICON - mc.font.lineHeight) / 2, nameColor, true);

            int iy = y + PAD + ICON + 4;
            int ix = x + PAD;

            // Input
            graphics.renderItem(furnaceItems.get(0), ix, iy); ix += ICON + 2;
            // Стрелка прогресса
            int cookPct = furnaceData[1] > 0 ? (int)(16f * furnaceData[0] / furnaceData[1]) : 0;
            graphics.fill(ix, iy + 4, ix + 16, iy + 12, 0x55AAAAAA);
            if (cookPct > 0) graphics.fill(ix, iy + 4, ix + cookPct, iy + 12, 0xFFFF8800);
            graphics.drawString(mc.font, "→", ix + 4, iy + 4, 0xFFFFFFFF, true);
            ix += 18;
            // Result
            graphics.renderItem(furnaceItems.get(2), ix, iy); ix += ICON + 8;
            // Топливо: иконка + количество предметов в слоте
            graphics.renderItem(furnaceItems.get(1), ix, iy); ix += ICON + 2;
            int fuelCount = furnaceItems.get(1).getCount();
            String fuelTxt = fuelCount > 0 ? "x" + fuelCount : "—";
            int fuelColor = fuelCount > 16 ? 0xFF55FF55 : fuelCount > 4 ? 0xFFFF9900 : 0xFFFF5555;
            if (fuelCount == 0) fuelColor = 0xFF777777;
            graphics.drawString(mc.font, fuelTxt, ix, iy + (ICON - mc.font.lineHeight) / 2, fuelColor, true);
            return;
        }

        // ── Обычный режим ────────────────────────────────────────────────
        int textStartX = x + PAD;

        if (hasIcon) {
            graphics.renderItem(icon, x + PAD, y + (totalH - ICON) / 2);
            textStartX = x + PAD + ICON + PAD;
        }

        if (hasPortrait) {
            float scale = PORT * 0.6f / Math.max(entityForPortrait.getBbWidth(), entityForPortrait.getBbHeight());
            try {
                net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsAngle(
                    graphics, x + PAD, y + PAD, x + PAD + PORT, y + PAD + PORT,
                    (int) scale, 0f, 0f, 0f, entityForPortrait);
            } catch (Exception ignored) {}
            textStartX = x + PAD + PORT + PAD;
        }

        int lines = 1 + (hasExtra ? 1 : 0) + (hasExtra2 ? 1 : 0);
        int textBlockH = mc.font.lineHeight * lines + (lines - 1) * 3;
        int nameY = y + (totalH - textBlockH) / 2;

        drawOutlined(graphics, mc, name,   textStartX, nameY, nameColor);
        if (hasExtra)  drawOutlined(graphics, mc, extra,  textStartX, nameY + mc.font.lineHeight + 3, 0xFFFF5555);
        if (hasExtra2) drawOutlined(graphics, mc, extra2, textStartX, nameY + (mc.font.lineHeight + 3) * 2, 0xFFFFAA00);
    }

    private static void drawOutlined(GuiGraphics g, Minecraft mc, String text, int x, int y, int color) {
        g.drawString(mc.font, text, x, y, color, true);
    }

    /** Читает package-private int поле через reflection по всей иерархии классов */
    private static int getIntField(Object obj, String fieldName) {
        try {
            Class<?> cls = obj.getClass();
            while (cls != null) {
                try {
                    java.lang.reflect.Field f = cls.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.getInt(obj);
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static final Map<String, String> ORE_RANGES = new HashMap<>();
    static {
        // Ванилла
        ORE_RANGES.put("minecraft:coal_ore",          "0 — 128");
        ORE_RANGES.put("minecraft:iron_ore",          "0 — 64");
        ORE_RANGES.put("minecraft:copper_ore",        "16 — 112");
        ORE_RANGES.put("minecraft:gold_ore",          "0 — 32");
        ORE_RANGES.put("minecraft:lapis_ore",         "0 — 32");
        ORE_RANGES.put("minecraft:redstone_ore",      "0 — 16");
        ORE_RANGES.put("minecraft:diamond_ore",       "0 — 16");
        ORE_RANGES.put("minecraft:emerald_ore",       "0 — 32");
        ORE_RANGES.put("minecraft:deepslate_coal_ore",     "0 — 128");
        ORE_RANGES.put("minecraft:deepslate_iron_ore",     "0 — 64");
        ORE_RANGES.put("minecraft:deepslate_copper_ore",   "16 — 112");
        ORE_RANGES.put("minecraft:deepslate_gold_ore",     "0 — 32");
        ORE_RANGES.put("minecraft:deepslate_lapis_ore",    "0 — 32");
        ORE_RANGES.put("minecraft:deepslate_redstone_ore", "0 — 16");
        ORE_RANGES.put("minecraft:deepslate_diamond_ore",  "0 — 16");
        ORE_RANGES.put("minecraft:deepslate_emerald_ore",  "0 — 32");
        // Industrial Upgrade - baseore
        ORE_RANGES.put("industrialupgrade:baseore/aluminium",  "0 — 80");
        ORE_RANGES.put("industrialupgrade:baseore/chromium",   "0 — 48");
        ORE_RANGES.put("industrialupgrade:baseore/cobalt",     "0 — 48");
        ORE_RANGES.put("industrialupgrade:baseore/germanium",  "0 — 32");
        ORE_RANGES.put("industrialupgrade:baseore/iridium",    "0 — 16");
        ORE_RANGES.put("industrialupgrade:baseore/magnesium",  "16 — 80");
        ORE_RANGES.put("industrialupgrade:baseore/manganese",  "0 — 64");
        ORE_RANGES.put("industrialupgrade:baseore/mikhail",    "0 — 32");
        ORE_RANGES.put("industrialupgrade:baseore/nickel",     "0 — 64");
        ORE_RANGES.put("industrialupgrade:baseore/platinum",   "0 — 32");
        ORE_RANGES.put("industrialupgrade:baseore/silver",     "0 — 48");
        ORE_RANGES.put("industrialupgrade:baseore/spinel",     "0 — 32");
        ORE_RANGES.put("industrialupgrade:baseore/titanium",   "0 — 48");
        ORE_RANGES.put("industrialupgrade:baseore/tungsten",   "0 — 32");
        ORE_RANGES.put("industrialupgrade:baseore/vanadium",   "0 — 48");
        ORE_RANGES.put("industrialupgrade:baseore/zinc",       "0 — 64");
        // Industrial Upgrade - classicore
        ORE_RANGES.put("industrialupgrade:classicore/copper",  "16 — 112");
        ORE_RANGES.put("industrialupgrade:classicore/lead",    "0 — 64");
        ORE_RANGES.put("industrialupgrade:classicore/tin",     "16 — 96");
        ORE_RANGES.put("industrialupgrade:classicore/uranium", "0 — 16");
    }

    private static String getOreSpawnRange(String blockId) {
        // Прямое совпадение
        if (ORE_RANGES.containsKey(blockId)) return ORE_RANGES.get(blockId);
        // Поиск по частичному совпадению (для blockspace руд и т.д.)
        String lower = blockId.toLowerCase();
        if (lower.contains("ore") || lower.contains("classicore") || lower.contains("baseore")) {
            for (Map.Entry<String, String> e : ORE_RANGES.entrySet()) {
                if (blockId.contains(e.getKey().split(":")[1])) return e.getValue();
            }
        }
        return null;
    }

}