package dev.muon.otherworldorigins.condition.entity;

import com.seniors.justlevelingfork.registry.RegistrySkills;
import com.seniors.justlevelingfork.registry.skills.Skill;
import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class HasSkillCondition {

    public static boolean condition(SerializableData.Instance data, Entity entity) {
        if (entity instanceof Player player) {
            ResourceLocation skillId = data.getId("skill");
            Skill skill = RegistrySkills.SKILLS_REGISTRY.get().getValue(skillId);
            return skill != null && skill.isEnabled(player);
        }
        return false;
    }

    public static ConditionFactory<Entity> getFactory() {
        return new ConditionFactory<>(
                OtherworldOrigins.loc("has_skill"),
                new SerializableData()
                        .add("skill", SerializableDataTypes.IDENTIFIER),
                HasSkillCondition::condition
        );
    }
}
