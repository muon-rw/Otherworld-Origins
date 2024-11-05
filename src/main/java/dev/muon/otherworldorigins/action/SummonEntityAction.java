package dev.muon.otherworldorigins.action;

import io.github.apace100.apoli.util.MiscUtil;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityAction;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import dev.muon.otherworldorigins.action.configuration.SummonEntityConfiguration;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.Optional;


public class SummonEntityAction extends EntityAction<SummonEntityConfiguration> {

    public SummonEntityAction() {
        super(SummonEntityConfiguration.CODEC);
    }
    @Override
    public void execute(SummonEntityConfiguration configuration, Entity caster) {
        if (!(caster.level() instanceof ServerLevel serverWorld))
            return;

        Optional<Entity> opt$entityToSpawn = MiscUtil.getEntityWithPassengers(
                serverWorld,
                configuration.entityType(),
                configuration.tag(),
                caster.position(),
                caster.getYRot(),
                caster.getXRot()
        );

        if (opt$entityToSpawn.isEmpty()) return;
        Entity entityToSpawn = opt$entityToSpawn.get();

        if (entityToSpawn instanceof Mob mob) {
            DifficultyInstance difficulty = serverWorld.getCurrentDifficultyAt(mob.blockPosition());
            MobSpawnType spawnType = MobSpawnType.MOB_SUMMONED;
            // but why
            ForgeEventFactory.onFinalizeSpawn(mob, serverWorld, difficulty, spawnType, null, configuration.tag());
            // mob.finalizeSpawn(serverWorld, difficulty, spawnType, null, configuration.tag());
            mob.setPersistenceRequired();
        }

        serverWorld.tryAddFreshEntityWithPassengers(entityToSpawn);
        ConfiguredEntityAction.execute(configuration.action(), entityToSpawn);


        ConfiguredBiEntityAction.execute(configuration.biEntityAction(), caster, entityToSpawn);

        configuration.weapon().ifPresent(weapon -> {
            entityToSpawn.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        });
    }
}
