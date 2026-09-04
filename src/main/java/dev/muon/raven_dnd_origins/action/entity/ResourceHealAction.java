package dev.muon.raven_dnd_origins.action.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.OptionalInt;

/**
 * Heals a living entity from a configured resource power's current value, consuming that resource
 * by the healed amount.
 */
public final class ResourceHealAction implements ActionType<EntityCtx, ResourceHealAction.Cfg> {

    public record Cfg(ResourceLocation resource) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                IdCodecs.ID.fieldOf("resource").forGetter(Cfg::resource)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity living = ctx.living();
        if (living == null) {
            return;
        }

        PowerContainer container = PowerContainer.of(living);
        if (container == null) {
            return;
        }

        OptionalInt available = PowerResources.read(container, cfg.resource());
        if (available.isEmpty() || available.getAsInt() <= 0) {
            return;
        }

        float missingHealth = living.getMaxHealth() - living.getHealth();
        int healAmount = (int) Math.min(missingHealth, available.getAsInt());
        if (healAmount <= 0) {
            return;
        }

        living.heal(healAmount);
        PowerResources.write(container, cfg.resource(), available.getAsInt() - healAmount);
    }
}
