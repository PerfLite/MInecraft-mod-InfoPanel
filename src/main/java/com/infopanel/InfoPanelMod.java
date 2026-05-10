package com.infopanel;

import com.infopanel.client.KeyBindings;
import com.infopanel.config.InfoPanelConfig;
import com.infopanel.event.HudRenderHandler;
import com.infopanel.event.LightOverlayRenderer;
import com.infopanel.event.SlimeChunkRenderer;
import com.infopanel.event.CompassRenderer;
import com.infopanel.event.StructureRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod("infopanel")
public class InfoPanelMod {

    public InfoPanelMod(IEventBus modEventBus) {
        modEventBus.addListener(this::registerKeys);
        modEventBus.addListener(this::clientSetup);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.TOGGLE_HUD);
        event.register(KeyBindings.OPEN_CONFIG);
        event.register(KeyBindings.TOGGLE_LIGHT_OVERLAY);
        event.register(KeyBindings.EDIT_LAYOUT);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        InfoPanelConfig.init();
        NeoForge.EVENT_BUS.register(new HudRenderHandler());
        NeoForge.EVENT_BUS.register(new LightOverlayRenderer());
        NeoForge.EVENT_BUS.register(new SlimeChunkRenderer());
        NeoForge.EVENT_BUS.register(new CompassRenderer());
        NeoForge.EVENT_BUS.register(new StructureRenderer());
        // PickupLogRenderer регистрируется автоматически через @EventBusSubscriber
    }
}
