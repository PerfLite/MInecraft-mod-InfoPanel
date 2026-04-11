package com.infopanel.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
            "key.infopanel.toggle",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.infopanel"
    );

    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.infopanel.config",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            "key.categories.infopanel"
    );

    public static final KeyMapping TOGGLE_LIGHT_OVERLAY = new KeyMapping(
            "key.infopanel.light_overlay",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F7,
            "key.categories.infopanel"
    );

    public static final KeyMapping TOGGLE_SLIME_CHUNKS = new KeyMapping(
            "key.infopanel.slime_chunks",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            "key.categories.infopanel"
    );

    public static final KeyMapping EDIT_LAYOUT = new KeyMapping(
            "key.infopanel.edit_layout",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            "key.categories.infopanel"
    );
}
