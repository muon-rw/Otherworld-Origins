package dev.muon.raven_dnd_origins.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class ModKeybinds {

    public static final KeyMapping CANTRIP_RACE_KEY = new KeyMapping(
            "key.raven_dnd_origins.cantrip_race",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.raven_dnd_origins"
    );
    public static final KeyMapping CANTRIP_ONE_KEY = new KeyMapping(
            "key.raven_dnd_origins.cantrip_one",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.raven_dnd_origins"
    );
    public static final KeyMapping CANTRIP_TWO_KEY = new KeyMapping(
            "key.raven_dnd_origins.cantrip_two",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "key.categories.raven_dnd_origins"
    );
    public static final KeyMapping CANTRIP_THREE_KEY = new KeyMapping(
            "key.raven_dnd_origins.cantrip_three",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.categories.raven_dnd_origins"
    );
    public static final KeyMapping TOGGLE_DARK_VISION_KEY = new KeyMapping(
            "key.raven_dnd_origins.toggle_dark_vision",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "key.categories.raven_dnd_origins"
    );
}
