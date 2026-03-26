package dev.muon.otherworldorigins.condition.entity;

import io.github.edwinmindcraft.apoli.common.condition.entity.IntComparingEntityCondition;
import net.minecraft.world.entity.Entity;

public class VelocityCondition extends IntComparingEntityCondition {

    public VelocityCondition() {
        super(VelocityCondition::getVelocityBps);
    }

    private static int getVelocityBps(Entity entity) {
        return (int) (entity.getDeltaMovement().length() * 20);
    }
}
