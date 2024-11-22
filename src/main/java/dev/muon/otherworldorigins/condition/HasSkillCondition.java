package dev.muon.otherworldorigins.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seniors.justlevelingfork.registry.RegistrySkills;
import com.seniors.justlevelingfork.registry.skills.Skill;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityCondition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class HasSkillCondition extends EntityCondition<HasSkillCondition.Configuration> {
    public HasSkillCondition() {
        super(Configuration.CODEC);
    }

    @Override
    public boolean check(@NotNull Configuration configuration, @NotNull Entity entity) {
        if (entity instanceof Player player) {
            Skill skill = RegistrySkills.getSkill(configuration.skill().getPath());
            return skill != null && skill.isEnabled(player);
        }
        return false;
    }

    public record Configuration(ResourceLocation skill) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("skill").forGetter(Configuration::skill)
        ).apply(instance, Configuration::new));
    }
}