package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Power that grants immunity to a specific spell. When a caster attempts to target this entity
 * with the specified spell via preCastTargetHelper (e.g. Root, Ray of Sickness), the target
 * is filtered out and the cast fails with "No valid target".
 */
public class SpellImmunityPower extends PowerType<SpellImmunityPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("spell").forGetter(Configuration::spell)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static boolean hasSpellImmunity(LivingEntity entity, ResourceLocation spellId) {
        if (entity == null || spellId == null) {
            return false;
        }
        boolean[] immune = {false};
        PowerLookup.forEach(entity, ModPowers.SPELL_IMMUNITY, Configuration.class, cfg -> {
            if (!immune[0] && spellIdsMatch(spellId, cfg.spell())) {
                immune[0] = true;
            }
        });
        return immune[0];
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

    public record Configuration(ResourceLocation spell) {
    }
}
