package dev.muon.otherworldorigins.condition.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.otherworld.leveling.LevelingUtils;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityCondition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class LeveledChanceCondition extends EntityCondition<LeveledChanceCondition.Configuration> {

    public LeveledChanceCondition() {
        super(Configuration.CODEC);
    }

    @Override
    public boolean check(@NotNull Configuration configuration, @NotNull Entity entity) {
        int level;
        if (configuration.aptitude().isPresent()) {
            ResourceLocation aptId = configuration.aptitude().get();
            Aptitude aptitude = RegistryAptitudes.getAptitude(aptId.getPath());
            if (aptitude == null) return false;
            if (entity instanceof Player player) {
                AptitudeCapability cap = AptitudeCapability.get(player);
                level = cap != null ? cap.getAptitudeLevel(aptitude) : 1;
            } else {
                level = 1;
            }
        } else {
            level = entity instanceof Player player ? LevelingUtils.getPlayerLevel(player) : 1;
        }
        float chance = configuration.base() + configuration.perLevel() * level;
        chance = Mth.clamp(chance, 0f, 1f);
        return entity.level().getRandom().nextFloat() < chance;
    }

    public record Configuration(float base, float perLevel, Optional<ResourceLocation> aptitude) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("base").forGetter(Configuration::base),
                Codec.FLOAT.fieldOf("per_level").forGetter(Configuration::perLevel),
                ResourceLocation.CODEC.optionalFieldOf("aptitude").forGetter(Configuration::aptitude)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            if (aptitude.isEmpty()) {
                return true;
            }
            return RegistryAptitudes.getAptitude(aptitude.get().getPath()) != null;
        }
    }
}
