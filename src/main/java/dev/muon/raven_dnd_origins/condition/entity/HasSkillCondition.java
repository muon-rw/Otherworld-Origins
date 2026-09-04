package dev.muon.raven_dnd_origins.condition.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seniors.justlevelingfork.registry.RegistrySkills;
import com.seniors.justlevelingfork.registry.skills.Skill;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class HasSkillCondition implements ConditionType<EntityCtx, HasSkillCondition.Cfg> {
    public record Cfg(ResourceLocation skill) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                IdCodecs.ID.fieldOf("skill").forGetter(Cfg::skill)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.raw() instanceof Player player)) return false;
        Skill skill = RegistrySkills.SKILLS_REGISTRY.getValue(cfg.skill());
        return skill != null && skill.isEnabled(player);
    }
}
