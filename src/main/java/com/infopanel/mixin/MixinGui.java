package com.infopanel.mixin;

import com.infopanel.event.EffectTimerRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Ordering;
import java.util.Collection;

@Mixin(Gui.class)
public class MixinGui {

    @Inject(method = "renderEffects", at = @At("TAIL"))
    private void onRenderEffects(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.options.hideGui) return;
        if (!com.infopanel.config.InfoPanelConfig.showEffectTimers) return;

        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        if (effects.isEmpty()) return;

        int screenW = graphics.guiWidth();
        int j1 = 0; // счётчик beneficial — как в ванили
        int k1 = 0; // счётчик harmful — как в ванили

        // Точно такая же сортировка как в ванили: Ordering.natural().reverse()
        for (MobEffectInstance effect : Ordering.natural().reverse().sortedCopy(effects)) {
            if (!effect.showIcon()) continue;

            int ex, ey;
            if (effect.getEffect().value().isBeneficial()) {
                ++j1;
                ex = screenW - 25 * j1;
                ey = 1;
            } else {
                ++k1;
                ex = screenW - 25 * k1;
                ey = 27; // 1 + 26, как в ванили
            }

            EffectTimerRenderer.renderSingleTimer(graphics, mc, effect, ex, ey, 24);
        }
    }
}
