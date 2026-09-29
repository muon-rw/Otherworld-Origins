package dev.muon.raven_dnd_origins.item;

import com.r3x.icarusrewinged.item.CustomTextureWingItem;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

/**
 * Icarus Rewinged membrane wings with a texture from this mod: {@link CustomTextureWingItem} only
 * resolves textures under its own namespace.
 */
public class DragonbornWingsItem extends CustomTextureWingItem {
    private final ResourceLocation texture;

    public DragonbornWingsItem(String textureName) {
        super(textureName, "leather", false, Rarity.EPIC, false, false, false, 0.0F, 0.0F);
        this.texture = RavenDndOrigins.loc("textures/entity/dragonborn/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getCustomLayer1() {
        return texture;
    }
}
