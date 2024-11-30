package dev.muon.otherworldorigins.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class ModKeybinds {

    public static final KeyMapping CANTRIP_ONE_KEY = new KeyMapping(
            "key.otherworldorigins.cantrip_one",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.categories.otherworldorigins"
    );
    public static final KeyMapping CANTRIP_TWO_KEY = new KeyMapping(
            "key.otherworldorigins.cantrip_two",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "key.categories.otherworldorigins"
    );
    public static final KeyMapping TOGGLE_DARK_VISION_KEY = new KeyMapping(
            "key.otherworldorigins.toggle_dark_vision",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "key.categories.otherworldorigins"
    );
}