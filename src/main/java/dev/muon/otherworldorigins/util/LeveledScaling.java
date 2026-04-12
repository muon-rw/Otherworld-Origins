package dev.muon.otherworldorigins.util;

import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.otherworld.leveling.LevelingUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Resolves the numeric level used by Otherworld leveled datapack math: {@link LevelingUtils} character
 * level, or a Just Leveling aptitude tier when {@code aptitude} is present. Non-players use {@code 1}.
 */
public final class LeveledScaling {
    private LeveledScaling() {}

    public static int levelForScaling(Entity entity, Optional<ResourceLocation> aptitude) {
        if (!(entity instanceof Player player)) {
            return 1;
        }
        if (aptitude.isEmpty()) {
            return LevelingUtils.getPlayerLevel(player);
        }
        Aptitude apt = RegistryAptitudes.getAptitude(aptitude.get().getPath());
        if (apt == null) {
            return 1;
        }
        AptitudeCapability cap = AptitudeCapability.get(player);
        return cap != null ? cap.getAptitudeLevel(apt) : 1;
    }

    /** {@code true} when {@code aptitude} is absent, or names a registered aptitude. */
    public static boolean isValidAptitudeReference(Optional<ResourceLocation> aptitude) {
        return aptitude.isEmpty() || RegistryAptitudes.getAptitude(aptitude.get().getPath()) != null;
    }
}
