package dev.muon.raven_dnd_origins.action.bientity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.OptionalInt;

public final class ResourceHealBientityAction implements ActionType<BiEntityCtx, ResourceHealBientityAction.Cfg> {

    public record Cfg(ResourceLocation resource) {}

    private static final MapCodec<Cfg> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("resource").forGetter(Cfg::resource)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> codec() {
        return CODEC;
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        LivingEntity target = ctx.livingTarget();
        Entity actor = ctx.actor();
        if (target == null || actor == null || ctx.level().isClientSide()) return;

        PowerContainer container = PowerContainer.of(actor);
        if (container == null) return;

        OptionalInt available = PowerResources.read(container, cfg.resource());
        if (available.isEmpty() || available.getAsInt() <= 0) return;

        float missingHealth = target.getMaxHealth() - target.getHealth();
        int healAmount = (int) Math.min(missingHealth, available.getAsInt());
        if (healAmount <= 0) return;

        target.heal(healAmount);
        PowerResources.write(container, cfg.resource(), available.getAsInt() - healAmount);
    }
}
