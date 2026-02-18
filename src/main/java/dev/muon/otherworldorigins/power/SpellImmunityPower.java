package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Power that grants immunity to a specific spell. When a caster attempts to target this entity
 * with the specified spell via preCastTargetHelper (e.g. Root, Ray of Sickness), the target
 * is filtered out and the cast fails with "No valid target".
 */
public class SpellImmunityPower extends PowerFactory<SpellImmunityPower.Configuration> {

    public SpellImmunityPower() {
        super(Configuration.CODEC);
    }

    /**
     * Returns true if the entity has spell immunity for the given spell ID.
     */
    public static boolean hasSpellImmunity(LivingEntity entity, ResourceLocation spellId) {
        if (entity == null || spellId == null) {
            return false;
        }
        return IPowerContainer.get(entity).resolve()
                .stream()
                .flatMap(container -> container.getPowers(ModPowers.SPELL_IMMUNITY.get()).stream())
                .anyMatch(holder -> {
                    ResourceLocation immuneSpell = holder.value().getConfiguration().spell();
                    return spellIdsMatch(spellId, immuneSpell);
                });
    }

    private static boolean spellIdsMatch(ResourceLocation a, ResourceLocation b) {
        if (a.equals(b)) {
            return true;
        }
        // Handle "acid_orb" vs "irons_spellbooks:acid_orb"
        String aStr = a.getNamespace().equals("minecraft") ? "irons_spellbooks:" + a.getPath() : a.toString();
        String bStr = b.getNamespace().equals("minecraft") ? "irons_spellbooks:" + b.getPath() : b.toString();
        return aStr.equals(bStr);
    }

    public record Configuration(ResourceLocation spell) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SerializableDataTypes.IDENTIFIER.fieldOf("spell").forGetter(Configuration::spell)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return spell != null;
        }
    }
}
