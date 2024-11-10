package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class CastingRestrictions {
    private static final List<String> ALLOWED_CLASSES = List.of(
            "warlock",
            "wizard",
            "sorcerer"
    );

    public static boolean isCastingAllowed(Player player) {
        return IOriginContainer.get(player).resolve().map(container -> {
            ResourceLocation classLayerLoc = OtherworldOrigins.loc("class");
            OriginLayer classLayer = OriginsAPI.getLayersRegistry().get(classLayerLoc);
            if (classLayer == null) {
                return false;
            }

            ResourceKey<Origin> playerOrigin = container.getOrigin(ResourceKey.create(OriginsAPI.getLayersRegistry().key(), classLayerLoc));
            return ALLOWED_CLASSES.stream().anyMatch(allowedClass ->
                    playerOrigin.location().equals(OtherworldOrigins.loc("class/" + allowedClass))
            );
        }).orElse(false);
    }


    public static String getRequiredClassesText() {
        if (ALLOWED_CLASSES.size() == 1) {
            return capitalize(ALLOWED_CLASSES.get(0)) + "s";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ALLOWED_CLASSES.size(); i++) {
            if (i > 0) {
                builder.append(i == ALLOWED_CLASSES.size() - 1 ? " or " : ", ");
            }
            builder.append(capitalize(ALLOWED_CLASSES.get(i))).append("s");
        }
        return builder.toString();
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}