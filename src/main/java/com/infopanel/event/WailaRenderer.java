package com.infopanel.event;

import com.infopanel.config.InfoPanelConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

        if (hit instanceof BlockHitResult blockHit) {
            BlockPos targetPos = blockHit.getBlockPos();
            BlockState state = mc.level.getBlockState(targetPos);
            if (state.isAir()) return;
            name = state.getBlock().getName().getString();
            icon = new ItemStack(state.getBlock().asItem());
            // Показываем высоту спавна если это руда
            String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getKey(state.getBlock()).toString();
            String spawnRange = getOreSpawnRange(blockId);
            if (spawnRange != null) extra = "Y: " + spawnRange;

        } else if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            name = entity.getName().getString();
            icon = ItemStack.EMPTY;
            if (entity instanceof LivingEntity living) {
                int hp    = (int) living.getHealth();
                int maxHp = (int) living.getMaxHealth();
                extra = "❤ " + hp + " / " + maxHp;
            }
        }

        if (name == null) return;
        renderWaila(event.getGuiGraphics(), mc, name, icon, extra);
    }

    private static void renderWaila(GuiGraphics graphics, Minecraft mc,
                                    String name, ItemStack icon, String extra) {
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int iconSize = 16;
        int padding  = 5;
        boolean hasIcon  = !icon.isEmpty();
        boolean hasExtra = extra != null;

        int textW  = mc.font.width(name);
        int extraW = hasExtra ? mc.font.width(extra) : 0;
        int totalW = padding + (hasIcon ? iconSize + padding : 0) + Math.max(textW, extraW) + padding;
        int totalH = (hasExtra ? mc.font.lineHeight * 2 + 4 : iconSize) + padding * 2;

        int margin  = 6;
        int hotbarH = 44;
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

        int textStartX = x + padding;
        if (hasIcon) {
            graphics.renderItem(icon, x + padding, y + padding);
            textStartX = x + padding + iconSize + padding;
        }

        int nameColor = 0xFF000000 | InfoPanelConfig.colorTargetBlock;
        int nameY = y + padding + (hasExtra ? 0 : (iconSize - mc.font.lineHeight) / 2);
        drawOutlined(graphics, mc, name, textStartX, nameY, nameColor);

        if (hasExtra) {
            drawOutlined(graphics, mc, extra, textStartX, nameY + mc.font.lineHeight + 3, 0xFFFF5555);
        }
    }

    private static void drawOutlined(GuiGraphics g, Minecraft mc, String text, int x, int y, int color) {
        g.drawString(mc.font, text, x, y, color, true);
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