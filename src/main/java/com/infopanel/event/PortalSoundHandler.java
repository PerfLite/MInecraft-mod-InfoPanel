package com.infopanel.event;

import com.infopanel.config.InfoPanelConfig;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "infopanel", value = Dist.CLIENT)
public class PortalSoundHandler {

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) return;

        String path = sound.getLocation().getPath();
        String ns   = sound.getLocation().getNamespace();

        // Логируем все звуки для отладки
        // org.apache.logging.log4j.LogManager.getLogger("infopanel").info("SOUND: {}:{}", ns, path);

        if (!"minecraft".equals(ns)) return;

        // block.portal.ambient — гул активного портала
        if ("block.portal.ambient".equals(path)) {
            if (!InfoPanelConfig.isPortalSoundEnabled()) {
                event.setSound(null);
                return;
            }
            float vol = InfoPanelConfig.getPortalSoundVolume();
            if (vol < 0.999f) {
                event.setSound(new VolumeScaledSound(sound, vol));
            }
            return;
        }

        // block.portal.trigger + block.portal.travel — звук при переходе
        if ("block.portal.trigger".equals(path) || "block.portal.travel".equals(path)) {
            if (!InfoPanelConfig.isPortalTravelEnabled()) {
                event.setSound(null);
                return;
            }
            float vol = InfoPanelConfig.getPortalTravelVolume();
            if (vol < 0.999f) {
                event.setSound(new VolumeScaledSound(sound, vol));
            }
        }
    }

    /** Обёртка над SoundInstance с изменённой громкостью */
    private static class VolumeScaledSound extends AbstractSoundInstance {
        private final SoundInstance original;
        private final float volumeScale;

        VolumeScaledSound(SoundInstance original, float volumeScale) {
            super(original.getLocation(), original.getSource(), RandomSource.create());
            this.original   = original;
            this.volumeScale = volumeScale;
            this.x        = original.getX();
            this.y        = original.getY();
            this.z        = original.getZ();
            this.looping  = original.isLooping();
            this.relative = original.isRelative();
            this.delay    = original.getDelay();
            this.attenuation = original.getAttenuation();
            this.volume   = volumeScale;
            this.pitch    = 1.0f;
        }

        @Override
        public float getVolume() {
            // Базовый getVolume() умножает на sound.getVolume() — но sound может быть null до resolve()
            // Возвращаем просто наш масштаб
            return volumeScale;
        }

        @Override
        public net.minecraft.client.sounds.WeighedSoundEvents resolve(net.minecraft.client.sounds.SoundManager manager) {
            // Делегируем resolve оригиналу чтобы получить правильный Sound объект
            var result = original.resolve(manager);
            this.sound = original.getSound();
            return result;
        }
    }
}
